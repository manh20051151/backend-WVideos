package com.example.backendWVideos.scheduler;

import com.example.backendWVideos.repository.NewsTranslationRepository;
import com.example.backendWVideos.service.GeminiApiClient;
import com.example.backendWVideos.service.NewsTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tự chữa lành cho bản dịch tin tức: định kỳ tìm các bài PUBLISHED còn thiếu
 * bản dịch (tạo/sửa xong nhưng lần dịch trước thất bại do lỗi mạng/Gemini
 * quá tải) rồi dịch lại. Bài đã đủ bản dịch sẽ không bị quét lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsTranslationScheduler {

    private final NewsTranslationRepository newsTranslationRepository;
    private final NewsTranslationService newsTranslationService;
    private final GeminiApiClient geminiApiClient;

    private static final int BATCH_SIZE = 10;

    @Scheduled(initialDelay = 180_000, fixedDelay = 900_000)
    public void retryIncompleteTranslations() {
        try {
            if (!geminiApiClient.isEnabled()) {
                return;
            }

            long targetCount = geminiApiClient.getTargetLocales().size();
            if (targetCount == 0) {
                return;
            }

            List<String> newsIds = newsTranslationRepository
                    .findNewsIdsWithIncompleteTranslations(targetCount, PageRequest.of(0, BATCH_SIZE));

            if (newsIds.isEmpty()) {
                return;
            }

            log.info("🌐 [NewsTranslationScheduler] Tìm thấy {} tin còn thiếu bản dịch, đang dịch lại",
                    newsIds.size());
            newsIds.forEach(newsTranslationService::translateNewsAsync);
        } catch (Exception e) {
            log.error("🌐 [NewsTranslationScheduler] Lỗi khi retry bản dịch: {}", e.getMessage());
        }
    }
}
