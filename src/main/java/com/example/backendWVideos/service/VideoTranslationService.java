package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.VideoTranslationResponse;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.entity.VideoTranslation;
import com.example.backendWVideos.repository.VideoRepository;
import com.example.backendWVideos.repository.VideoTranslationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dịch tự động tiêu đề + mô tả video sang nhiều ngôn ngữ bằng Gemini API
 * (Google AI Studio, free tier) sau khi upload thành công.
 *
 * - Bản gốc là tiếng Việt (title/description trên bảng videos).
 * - Kết quả lưu vào bảng video_translations, mỗi cặp (video, locale) 1 dòng,
 *   dịch lại thì ghi đè (upsert).
 * - Không có API key trong config -> tự tắt (log warn, không lỗi).
 * - Việc gọi Gemini/kiểm tra key/thử lại khi quá tải nằm ở GeminiApiClient dùng chung.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranslationService {

    private final VideoRepository videoRepository;
    private final VideoTranslationRepository videoTranslationRepository;
    private final GeminiApiClient geminiApiClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Dịch video nền (async): gọi sau khi upload thành công.
     * An toàn gọi lại nhiều lần - nếu đã đủ bản dịch thì bỏ qua.
     */
    @Async("taskExecutor")
    public void translateVideoAsync(String videoId) {
        try {
            if (!geminiApiClient.isEnabled()) {
                log.warn("🌐 [Translation] Bỏ qua dịch video {}: chưa cấu hình app.translation.gemini-api-key", videoId);
                return;
            }

            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null || video.getTitle() == null || video.getTitle().isBlank()) {
                return;
            }

            List<String> targets = geminiApiClient.getTargetLocales();
            if (targets.isEmpty()) {
                return;
            }

            // Đã đủ bản dịch cho mọi ngôn ngữ đích thì bỏ qua
            if (videoTranslationRepository.countByVideoId(videoId) >= targets.size()) {
                log.info("🌐 [Translation] Video {} đã có đủ bản dịch, bỏ qua", videoId);
                return;
            }

            log.info("🌐 [Translation] Bắt đầu dịch video {} sang {} ngôn ngữ: {}", videoId, targets.size(), targets);

            String prompt = buildPrompt(video, targets);
            String json = geminiApiClient.generateJson(prompt);
            if (json == null) {
                return;
            }

            JsonNode root = MAPPER.readTree(json);
            int saved = 0;
            for (String locale : targets) {
                JsonNode entry = root.get(locale);
                if (entry == null || entry.get("title") == null || entry.get("title").asText().isBlank()) {
                    continue;
                }
                String title = entry.get("title").asText();
                String description = entry.hasNonNull("description") ? entry.get("description").asText() : "";

                VideoTranslation translation = videoTranslationRepository
                        .findByVideoIdAndLocale(videoId, locale)
                        .orElseGet(() -> VideoTranslation.builder().video(video).locale(locale).build());
                translation.setTitle(title);
                translation.setDescription(description);
                videoTranslationRepository.save(translation);
                saved++;
            }
            log.info("🌐 [Translation] ✅ Dịch xong video {}: {}/{} ngôn ngữ", videoId, saved, targets.size());

        } catch (Exception e) {
            // Job nền - không bao giờ làm hỏng luồng chính, chỉ log
            log.error("🌐 [Translation] ❌ Lỗi khi dịch video {}: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * Dịch lại khi user sửa tiêu đề/mô tả: xóa bản dịch cũ rồi dịch lại (sau commit).
     */
    public void scheduleRetranslate(String videoId) {
        if (!geminiApiClient.isEnabled()) {
            return;
        }
        videoTranslationRepository.deleteByVideoId(videoId);
        log.info("🌐 [Translation] Đã xóa bản dịch cũ của video {}, sẽ dịch lại sau commit", videoId);

        // Đăng ký chạy sau commit để thread async đọc được dữ liệu mới nhất
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    translateVideoAsync(videoId);
                }
            });
        } else {
            translateVideoAsync(videoId);
        }
    }

    /**
     * Lấy danh sách bản dịch của video (public) - hỗ trợ cả id và slug.
     */
    public List<VideoTranslationResponse> getTranslations(String idOrSlug) {
        Video video = videoRepository.findById(idOrSlug)
                .or(() -> videoRepository.findBySlug(idOrSlug))
                .orElse(null);
        if (video == null) {
            return List.of();
        }
        return videoTranslationRepository.findByVideoIdOrderByLocaleAsc(video.getId()).stream()
                .map(t -> VideoTranslationResponse.builder()
                        .locale(t.getLocale())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private String buildPrompt(Video video, List<String> targets) {
        String description = video.getDescription() == null ? "" : video.getDescription();
        return """
                Translate a video's title and description from Vietnamese into these languages: %s

                Rules:
                - Keep proper nouns, brand names, URLs and numbers unchanged.
                - If the description is empty, translate it as an empty string.
                - Keep translations natural and concise, suitable for a video sharing platform.

                Title: %s
                Description: %s

                Respond with ONLY a JSON object (no markdown, no extra text) in this exact format:
                {"en":{"title":"...","description":"..."},"zh-CN":{"title":"...","description":"..."}}
                with one entry for every requested language code.
                """.formatted(String.join(", ", targets), video.getTitle(), description);
    }

    /**
     * Danh sách ngôn ngữ hệ thống đang hỗ trợ (cho frontend hiển thị lựa chọn).
     */
    public List<String> getSupportedLocales() {
        List<String> locales = new ArrayList<>();
        locales.add("vi");
        locales.addAll(geminiApiClient.getTargetLocales());
        return locales;
    }

    /**
     * i18n bản dịch có đang bật không (đã cấu hình Gemini API key)?
     */
    public boolean isTranslationEnabled() {
        return geminiApiClient.isEnabled();
    }

    /**
     * Số ngôn ngữ đích cần dịch cho mỗi video (không tính tiếng Việt gốc).
     */
    public int getTargetLocaleCount() {
        return geminiApiClient.getTargetLocales().size();
    }
}
