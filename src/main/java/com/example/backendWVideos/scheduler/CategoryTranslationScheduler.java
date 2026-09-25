package com.example.backendWVideos.scheduler;

import com.example.backendWVideos.service.CategoryTranslationService;
import com.example.backendWVideos.service.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tự chữa lành cho bản dịch danh mục: định kỳ quét các danh mục (thể loại video
 * + danh mục tin tức) còn thiếu bản dịch rồi dịch lại batch 1 lần gọi.
 * Danh mục là tập nhỏ nên quét toàn bộ mỗi lần với chi phí không đáng kể.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryTranslationScheduler {

    private final CategoryTranslationService categoryTranslationService;
    private final GeminiApiClient geminiApiClient;

    @Scheduled(initialDelay = 120_000, fixedDelay = 900_000)
    public void backfillMissingTranslations() {
        try {
            if (!geminiApiClient.isEnabled()) {
                return;
            }
            categoryTranslationService.translateMissingCategoriesAsync();
        } catch (Exception e) {
            log.error("🌐 [CategoryTranslationScheduler] Lỗi: {}", e.getMessage());
        }
    }
}
