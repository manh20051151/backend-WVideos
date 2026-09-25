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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch tự động tiêu đề + mô tả video sang nhiều ngôn ngữ bằng Gemini API
 * (Google AI Studio, free tier) sau khi upload thành công.
 *
 * - Bản gốc là tiếng Việt (title/description trên bảng videos).
 * - Kết quả lưu vào bảng video_translations, mỗi cặp (video, locale) 1 dòng,
 *   dịch lại thì ghi đè (upsert).
 * - Không có API key trong config -> tự tắt (log warn, không lỗi).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranslationService {

    private final VideoRepository videoRepository;
    private final VideoTranslationRepository videoTranslationRepository;

    @Value("${app.translation.gemini-api-key:}")
    private String geminiApiKey;

    @Value("${app.translation.gemini-model:gemini-3.5-flash-lite}")
    private String geminiModel;

    // Các ngôn ngữ đích, phân tách bằng dấu phẩy. Bỏ "vi" vì là ngôn ngữ gốc.
    @Value("${app.translation.target-locales:en,zh-CN,ja,ko,hi,th,lo,km}")
    private String targetLocalesConfig;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private List<String> targetLocales() {
        return Arrays.stream(targetLocalesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("vi"))
                .collect(Collectors.toList());
    }

    /**
     * Dịch video nền (async): gọi sau khi upload thành công.
     * An toàn gọi lại nhiều lần - nếu đã đủ bản dịch thì bỏ qua.
     */
    @Async("taskExecutor")
    public void translateVideoAsync(String videoId) {
        try {
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                log.warn("🌐 [Translation] Bỏ qua dịch video {}: chưa cấu hình app.translation.gemini-api-key", videoId);
                return;
            }

            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null || video.getTitle() == null || video.getTitle().isBlank()) {
                return;
            }

            List<String> targets = targetLocales();
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
            String json = callGemini(prompt);
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
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
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
     * Gọi Gemini generateContent (REST, dùng API key từ Google AI Studio).
     * Model free tier thỉnh thoảng quá tải (HTTP 503) -> thử lại tối đa 3 lần.
     * Trả về text JSON hoặc null nếu lỗi.
     */
    private String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.2)
        );

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        String payload;
        try {
            payload = MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            log.error("🌐 [Translation] Không serialize được request body: {}", e.getMessage());
            return null;
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 503 || response.statusCode() == 429) {
                    log.warn("🌐 [Translation] Gemini quá tải (HTTP {}), thử lại lần {}/3 sau 10s",
                            response.statusCode(), attempt);
                    Thread.sleep(10_000);
                    continue;
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null) {
                    log.error("🌐 [Translation] Gemini trả về HTTP {}: {}", response.statusCode(),
                            response.body() != null ? response.body().substring(0,
                                    Math.min(300, response.body().length())) : "(empty)");
                    return null;
                }

                JsonNode root = MAPPER.readTree(response.body());
                JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
                if (!parts.isArray() || parts.isEmpty()) {
                    log.error("🌐 [Translation] Gemini response không có parts: {}", response.body());
                    return null;
                }
                return parts.path(0).path("text").asText();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                // Timeout/mạng nghẽn là lỗi tạm thời -> thử lại thay vì bỏ cả job
                if (attempt < 3) {
                    log.warn("🌐 [Translation] Lỗi tạm thời gọi Gemini (lần {}): {} -> thử lại sau 10s",
                            attempt, e.getMessage());
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    continue;
                }
                log.error("🌐 [Translation] Lỗi gọi Gemini API sau 3 lần thử: {}", e.getMessage());
                return null;
            }
        }
        log.error("🌐 [Translation] Gemini vẫn không phản hồi sau 3 lần thử, bỏ qua lần dịch này");
        return null;
    }

    /**
     * Danh sách ngôn ngữ hệ thống đang hỗ trợ (cho frontend hiển thị lựa chọn).
     */
    public List<String> getSupportedLocales() {
        List<String> locales = new ArrayList<>();
        locales.add("vi");
        locales.addAll(targetLocales());
        return locales;
    }

    /**
     * i18n bản dịch có đang bật không (đã cấu hình Gemini API key)?
     */
    public boolean isTranslationEnabled() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    /**
     * Số ngôn ngữ đích cần dịch cho mỗi video (không tính tiếng Việt gốc).
     */
    public int getTargetLocaleCount() {
        return targetLocales().size();
    }
}
