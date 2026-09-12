package com.example.backendWVideos.service;

import com.example.backendWVideos.entity.VideoViewLog;
import com.example.backendWVideos.repository.VideoViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoViewLogService {

    private final VideoViewLogRepository videoViewLogRepository;

    /**
     * Ghi log mỗi lượt xem (bất đồng bộ, transaction riêng).
     * Bắt ngoại lệ để không ảnh hưởng đến luồng tăng view chính.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logView(String videoId, String ip) {
        try {
            videoViewLogRepository.save(VideoViewLog.builder()
                    .videoId(videoId)
                    .viewedAt(LocalDateTime.now())
                    .ip(ip)
                    .build());
        } catch (Exception e) {
            log.debug("⚠️ Lỗi ghi log lượt xem (bỏ qua): {}", e.getMessage());
        }
    }
}
