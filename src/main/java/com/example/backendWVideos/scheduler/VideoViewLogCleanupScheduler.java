package com.example.backendWVideos.scheduler;

import com.example.backendWVideos.repository.VideoViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoViewLogCleanupScheduler {

    private final VideoViewLogRepository videoViewLogRepository;

    /**
     * Dọn các log lượt xem cũ (>7 ngày) để bảng không phình, chạy 03:00 mỗi ngày.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime before = LocalDateTime.now().minusDays(7);
        int deleted = videoViewLogRepository.deleteBefore(before);
        log.info("🧹 [Scheduler] Đã xóa {} video_view_logs cũ (>7 ngày)", deleted);
    }
}
