package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.NewsTranslationUpsertRequest;
import com.example.backendWVideos.dto.response.NewsTranslationResponse;
import com.example.backendWVideos.entity.News;
import com.example.backendWVideos.entity.NewsTranslation;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.NewsRepository;
import com.example.backendWVideos.repository.NewsTranslationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch tự động bài tin tức (tiêu đề + tóm tắt + nội dung HTML) sang nhiều ngôn ngữ
 * bằng Gemini sau khi admin tạo/sửa bài.
 *
 * - Bản gốc là tiếng Việt (title/summary/content trên bảng news).
 * - Kết quả lưu vào bảng news_translations, mỗi cặp (news, locale) 1 dòng.
 * - Sửa nội dung gốc -> xóa bản dịch cũ rồi dịch lại (sau commit).
 * - Không có API key trong config -> tự tắt (log warn, không lỗi).
 * - Việc gọi Gemini/kiểm tra key/thử lại khi quá tải nằm ở GeminiApiClient dùng chung.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsTranslationService {

    private final NewsRepository newsRepository;
    private final NewsTranslationRepository newsTranslationRepository;
    private final GeminiApiClient geminiApiClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Dịch bài tin tức (async): gọi sau khi tạo/sửa bài.
     * An toàn gọi lại nhiều lần - nếu đã đủ bản dịch thì bỏ qua.
     */
    @Async("taskExecutor")
    public void translateNewsAsync(String newsId) {
        try {
            if (!geminiApiClient.isEnabled()) {
                log.warn("🌐 [NewsTranslation] Bỏ qua dịch tin {}: chưa cấu hình app.translation.gemini-api-key", newsId);
                return;
            }

            News news = newsRepository.findById(newsId).orElse(null);
            if (news == null || news.getTitle() == null || news.getTitle().isBlank()) {
                return;
            }

            List<String> targets = geminiApiClient.getTargetLocales();
            if (targets.isEmpty()) {
                return;
            }

            // Đã đủ bản dịch cho mọi ngôn ngữ đích thì bỏ qua
            if (newsTranslationRepository.countByNewsId(newsId) >= targets.size()) {
                log.info("🌐 [NewsTranslation] Tin {} đã có đủ bản dịch, bỏ qua", newsId);
                return;
            }

            log.info("🌐 [NewsTranslation] Bắt đầu dịch tin {} sang {} ngôn ngữ", newsId, targets.size());

            String prompt = buildPrompt(news, targets);
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

                // Bỏ qua ngôn ngữ đã có bản dịch (bảo vệ bản dịch admin đã sửa tay);
                // luồng dịch lại (scheduleRetranslate) đã xóa sạch trước nên vẫn dịch đủ
                if (newsTranslationRepository.findByNewsIdAndLocale(newsId, locale).isPresent()) {
                    continue;
                }

                NewsTranslation translation = NewsTranslation.builder().news(news).locale(locale).build();
                translation.setTitle(entry.get("title").asText());
                translation.setSummary(entry.hasNonNull("summary") ? entry.get("summary").asText() : "");
                translation.setContent(entry.hasNonNull("content") ? entry.get("content").asText() : "");
                newsTranslationRepository.save(translation);
                saved++;
            }
            log.info("🌐 [NewsTranslation] ✅ Dịch xong tin {}: {}/{} ngôn ngữ", newsId, saved, targets.size());

        } catch (Exception e) {
            // Job nền - không bao giờ làm hỏng luồng chính, chỉ log
            log.error("🌐 [NewsTranslation] ❌ Lỗi khi dịch tin {}: {}", newsId, e.getMessage(), e);
        }
    }

    /**
     * Gọi trong transaction sau khi tạo tin - dịch sau khi commit.
     */
    public void scheduleTranslate(String newsId) {
        registerAfterCommit(() -> translateNewsAsync(newsId));
    }

    /**
     * Sửa tiêu đề/tóm tắt/nội dung -> xóa bản dịch cũ, dịch lại sau commit.
     */
    public void scheduleRetranslate(String newsId) {
        if (!geminiApiClient.isEnabled()) {
            return;
        }
        newsTranslationRepository.deleteByNewsId(newsId);
        log.info("🌐 [NewsTranslation] Đã xóa bản dịch cũ của tin {}, sẽ dịch lại sau commit", newsId);
        registerAfterCommit(() -> translateNewsAsync(newsId));
    }

    /**
     * Xóa tin -> dọn bản dịch tránh bản mồ côi.
     */
    public void deleteTranslations(String newsId) {
        newsTranslationRepository.deleteByNewsId(newsId);
    }

    /**
     * Danh sách bản dịch của 1 bài tin cho trang admin quản lý:
     * trả đủ mọi ngôn ngữ đích, ngôn ngữ chưa dịch thì các trường = null.
     */
    public List<NewsTranslationResponse> getTranslationsForAdmin(String newsId) {
        List<String> targets = geminiApiClient.getTargetLocales();
        if (targets.isEmpty()) {
            return List.of();
        }
        Map<String, NewsTranslation> existing = newsTranslationRepository.findByNewsId(newsId).stream()
                .collect(Collectors.toMap(NewsTranslation::getLocale, t -> t));
        return targets.stream()
                .map(locale -> {
                    NewsTranslation t = existing.get(locale);
                    return NewsTranslationResponse.builder()
                            .locale(locale)
                            .title(t != null ? t.getTitle() : null)
                            .summary(t != null ? t.getSummary() : null)
                            .content(t != null ? t.getContent() : null)
                            .updatedAt(t != null ? t.getUpdatedAt() : null)
                            .build();
                })
                .toList();
    }

    /**
     * Admin cập nhật bản dịch bài tin sau khi Gemini dịch (sửa lại cho tự nhiên,
     * dịch tay ngôn ngữ còn thiếu, hoặc xóa bản dịch sai - title để trống).
     */
    @org.springframework.transaction.annotation.Transactional
    public List<NewsTranslationResponse> updateTranslations(
            String newsId, List<NewsTranslationUpsertRequest.Item> items) {
        List<String> targets = geminiApiClient.getTargetLocales();
        for (NewsTranslationUpsertRequest.Item item : items) {
            if (item.getLocale() == null || targets.stream().noneMatch(t -> t.equalsIgnoreCase(item.getLocale()))) {
                throw new AppException(ErrorCode.UNSUPPORTED_TRANSLATION_LOCALE);
            }
        }

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_NOT_FOUND));

        for (NewsTranslationUpsertRequest.Item item : items) {
            String locale = targets.stream()
                    .filter(t -> t.equalsIgnoreCase(item.getLocale())).findFirst().orElseThrow();
            String title = item.getTitle() != null ? item.getTitle().trim() : "";

            if (title.isEmpty()) {
                // Title rỗng -> xóa bản dịch, frontend sẽ fallback nội dung tiếng Việt gốc
                newsTranslationRepository.findByNewsIdAndLocale(newsId, locale)
                        .ifPresent(newsTranslationRepository::delete);
                continue;
            }

            NewsTranslation t = newsTranslationRepository.findByNewsIdAndLocale(newsId, locale)
                    .orElseGet(() -> NewsTranslation.builder().news(news).locale(locale).build());
            t.setTitle(title);
            t.setSummary(item.getSummary() != null ? item.getSummary() : "");
            t.setContent(item.getContent() != null ? item.getContent() : "");
            newsTranslationRepository.save(t);
        }
        log.info("🌐 [NewsTranslation] Admin cập nhật {} bản dịch cho tin {}", items.size(), newsId);

        return getTranslationsForAdmin(newsId);
    }

    /**
     * Bản dịch đã bản địa hóa cho danh sách tin theo locale của request.
     * Trả Map<newsId, bản dịch>; tin không có bản dịch thì không có trong map
     * (frontend/backend fallback nội dung tiếng Việt gốc).
     */
    public Map<String, NewsTranslation> getLocalizedTranslations(List<String> newsIds, Locale locale) {
        if (newsIds == null || newsIds.isEmpty() || locale == null) {
            return Map.of();
        }
        String contentLocale = resolveContentLocale(locale);
        if (contentLocale == null) {
            return Map.of();
        }
        return newsTranslationRepository
                .findByNewsIdInAndLocale(newsIds, contentLocale).stream()
                .collect(Collectors.toMap(t -> t.getNews().getId(), t -> t));
    }

    /**
     * Chuẩn hóa locale của request về mã ngôn ngữ bản dịch đang lưu
     * (vd "zh-TW" -> "zh-CN", null nếu ngôn ngữ không có bản dịch).
     */
    private String resolveContentLocale(Locale locale) {
        List<String> targets = geminiApiClient.getTargetLocales();
        for (String t : targets) {
            if (t.equalsIgnoreCase(locale.toString())) {
                return t;
            }
        }
        String lang = locale.getLanguage();
        for (String t : targets) {
            if (t.equalsIgnoreCase(lang) || t.toLowerCase().startsWith(lang.toLowerCase() + "-")) {
                return t;
            }
        }
        return null;
    }

    private String buildPrompt(News news, List<String> targets) {
        String summary = news.getSummary() == null ? "" : news.getSummary();
        String content = news.getContent() == null ? "" : news.getContent();
        return """
                Translate a news article's title, summary and HTML content from Vietnamese into these languages: %s

                Rules:
                - Keep proper nouns, brand names, URLs and numbers unchanged.
                - The content is HTML: keep all HTML tags and attributes exactly as they are, only translate the visible text.
                - Keep the summary short (1-2 sentences), suitable for a news listing.
                - If the summary is empty, translate it as an empty string.

                Title: %s
                Summary: %s
                Content: %s

                Respond with ONLY a JSON object (no markdown, no extra text) in this exact format:
                {"en":{"title":"...","summary":"...","content":"..."},"zh-CN":{"title":"...","summary":"...","content":"..."}}
                with one entry for every requested language code.
                """.formatted(String.join(", ", targets), news.getTitle(), summary, content);
    }

    private void registerAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
