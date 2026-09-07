package com.example.backendWVideos.scheduler;

import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.VideoStatus;
import com.example.backendWVideos.repository.VideoRepository;
import com.example.backendWVideos.service.StreamtapeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoStatusSyncScheduler {

    private final VideoRepository videoRepository;
    private final StreamtapeService streamtapeService;

    private static final String STREAMTAPE = "streamtape";
    private static final int BATCH_SIZE = 20;

    /**
     * Tự động re-check các video Streamtape đang ở trạng thái PROCESSING.
     * Streamtape cần thời gian convert sau khi upload, nên status chỉ cập nhật
     * thành READY khi convert xong (converted = true).
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void syncProcessingVideos() {
        List<Video> processing = videoRepository
                .findByStatus(VideoStatus.PROCESSING, PageRequest.of(0, BATCH_SIZE))
                .getContent();

        if (processing.isEmpty()) {
            return;
        }

        log.info("🔄 [Scheduler] Đang re-check {} video PROCESSING", processing.size());

        for (Video video : processing) {
            try {
                // Chỉ xử lý video Streamtape (provider null được coi là cần recover)
                if (STREAMTAPE.equals(video.getProvider())) {
                    // đúng Streamtape -> tiếp tục
                } else if (video.getProvider() != null) {
                    // provider rõ ràng khác (vd: doodstream) -> bỏ qua
                    continue;
                }

                if (video.getFileCode() == null) {
                    continue;
                }

                Map<String, Object> fileInfo = streamtapeService.getFileInfo(video.getFileCode());
                if (fileInfo == null || fileInfo.get("result") == null) {
                    continue;
                }

                Map<String, Object> resultMap = (Map<String, Object>) fileInfo.get("result");
                Map<String, Object> info = (Map<String, Object>) resultMap.get(video.getFileCode());
                if (info == null) {
                    continue;
                }

                boolean converted = info.get("converted") != null
                        && Boolean.parseBoolean(info.get("converted").toString());

                if (converted) {
                    video.setStatus(VideoStatus.READY);

                    if (info.get("size") != null) {
                        video.setFileSize(Long.parseLong(info.get("size").toString()));
                    }

                    // Chỉ ghi đè thumbnail nếu chưa có hoặc là URL Streamtape tự động
                    // (giữ nguyên thumbnail tùy chỉnh do user cung cấp)
                    String currentThumb = video.getThumbnailUrl();
                    boolean isAutoThumb = currentThumb == null || currentThumb.isEmpty()
                            || currentThumb.contains("thumb.tapecontent.net/thumb/");
                    if (isAutoThumb) {
                        try {
                            String splash = streamtapeService.getSplashImage(video.getFileCode());
                            if (splash != null && !splash.isEmpty()) {
                                video.setThumbnailUrl(splash);
                                video.setSplashImageUrl(splash);
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ [Scheduler] Không lấy được splash image cho video {}: {}",
                                    video.getId(), e.getMessage());
                        }
                    }

                    log.info("✅ [Scheduler] Video {} đã convert xong -> READY", video.getId());
                } else {
                    log.info("⏳ [Scheduler] Video {} vẫn đang convert...", video.getId());
                }
            } catch (Exception e) {
                log.warn("⚠️ [Scheduler] Lỗi re-check video {}: {}", video.getId(), e.getMessage());
            }
        }
    }
}
