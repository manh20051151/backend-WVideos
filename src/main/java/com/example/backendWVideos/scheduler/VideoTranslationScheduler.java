package com.example.backendWVideos.scheduler;

import com.example.backendWVideos.enums.VideoStatus;
import com.example.backendWVideos.repository.VideoTranslationRepository;
import com.example.backendWVideos.service.VideoTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tự chữa lành cho bản dịch video: định kỳ tìm các video còn thiếu bản dịch
 * (upload xong nhưng lần dịch trước thất bại do lỗi mạng/Gemini quá tải)
 * rồi dịch lại. Video đã đủ bản dịch sẽ không bị quét lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTranslationScheduler {

    private final VideoTranslationRepository videoTranslationRepository;
    private final VideoTranslationService videoTranslationService;

    private static final int BATCH_SIZE = 20;

    @Scheduled(initialDelay = 120_000, fixedDelay = 900_000)
    public void retryIncompleteTranslations() {
        try {
            if (!videoTranslationService.isTranslationEnabled()) {
                return;
            }

            long targetCount = videoTranslationService.getTargetLocaleCount();
            if (targetCount == 0) {
                return;
            }

            List<String> videoIds = videoTranslationRepository.findVideoIdsWithIncompleteTranslations(
                    List.of(VideoStatus.READY, VideoStatus.PROCESSING),
                    targetCount,
                    PageRequest.of(0, BATCH_SIZE));

            if (videoIds.isEmpty()) {
                return;
            }

            log.info("🌐 [TranslationScheduler] Tìm thấy {} video còn thiếu bản dịch, đang dịch lại",
                    videoIds.size());
            videoIds.forEach(videoTranslationService::translateVideoAsync);
        } catch (Exception e) {
            log.error("🌐 [TranslationScheduler] Lỗi khi retry bản dịch: {}", e.getMessage());
        }
    }
}
