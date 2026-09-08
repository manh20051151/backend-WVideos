package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.StreamtapeUploadResult;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.VideoStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamtapeUploadAsyncService {

    private final VideoRepository videoRepository;
    private final StreamtapeService streamtapeService;

    // Thư mục lưu file tạm
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/wvideos-uploads/";

    static {
        File dir = new File(TEMP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void uploadToStreamtapeAsync(String videoId, String tempFilePath, String originalFilename, String customThumbnailUrl) {
        log.info("🔄 [Streamtape - New Thread] Bắt đầu upload video {} lên Streamtape", videoId);

        File tempFile = null;
        try {
            tempFile = new File(tempFilePath);
            if (!tempFile.exists()) {
                throw new AppException(ErrorCode.UPLOAD_FAILED);
            }

            byte[] fileBytes = Files.readAllBytes(tempFile.toPath());
            log.info("📦 [Streamtape] Đã đọc {} bytes từ file tạm: {}", fileBytes.length, tempFilePath);

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

            // Lấy upload URL từ Streamtape
            String uploadUrl = streamtapeService.getUploadUrl();
            log.info("🔄 [Streamtape - New Thread] Upload URL: {}", uploadUrl);

            // Upload file lên Streamtape
            StreamtapeUploadResult uploadResult = streamtapeService.uploadFileBytes(
                    fileBytes, originalFilename, uploadUrl);
            log.info("✅ [Streamtape - New Thread] Upload thành công, fileId: {}", uploadResult.getFileId());

            // Cập nhật thông tin cơ bản từ kết quả upload
            video.setFileCode(uploadResult.getFileId());
            video.setEmbedUrl(uploadResult.getEmbedUrl());

            // Lấy thông tin chi tiết (size, trạng thái convert) qua file/info
            try {
                java.util.Map<String, Object> fileInfo = streamtapeService.getFileInfo(uploadResult.getFileId());
                if (fileInfo != null && fileInfo.get("result") != null) {
                    java.util.Map<String, Object> resultMap =
                            (java.util.Map<String, Object>) fileInfo.get("result");
                    java.util.Map<String, Object> info =
                            (java.util.Map<String, Object>) resultMap.get(uploadResult.getFileId());

                    if (info != null) {
                        if (info.get("size") != null) {
                            video.setFileSize(Long.parseLong(info.get("size").toString()));
                        }
                        if (info.get("converted") != null) {
                            boolean converted = Boolean.parseBoolean(info.get("converted").toString());
                            video.setStatus(converted ? VideoStatus.READY : VideoStatus.PROCESSING);
                        } else {
                            video.setStatus(VideoStatus.PROCESSING);
                        }
                    } else {
                        video.setStatus(VideoStatus.PROCESSING);
                    }
                } else {
                    video.setStatus(VideoStatus.PROCESSING);
                }
            } catch (Exception e) {
                log.warn("⚠️ [Streamtape] Không lấy được file info, để status PROCESSING: {}", e.getMessage());
                video.setStatus(VideoStatus.PROCESSING);
            }

            // Xử lý thumbnail - Streamtape không trả ảnh trong kết quả upload
            try {
                // Luôn lấy splash image từ Streamtape để làm ảnh hover khi có splashImageUrl
                String splash = streamtapeService.getSplashImage(uploadResult.getFileId());
                if (customThumbnailUrl != null && !customThumbnailUrl.isEmpty()) {
                    log.info("🖼️ [Streamtape - New Thread] Sử dụng thumbnail tùy chỉnh: {}", customThumbnailUrl);
                    video.setThumbnailUrl(customThumbnailUrl);
                    if (splash != null && !splash.isEmpty()) {
                        video.setSplashImageUrl(splash);
                        log.info("🖼️ [Streamtape - New Thread] Đã set splash image (hover): {}", splash);
                    }
                } else {
                    if (splash != null && !splash.isEmpty()) {
                        video.setThumbnailUrl(splash);
                        video.setSplashImageUrl(splash);
                        log.info("🖼️ [Streamtape - New Thread] Đã set thumbnail: {}", splash);
                    } else {
                        log.warn("⚠️ [Streamtape - New Thread] Splash image null/empty cho file: {}", uploadResult.getFileId());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [Streamtape] Không lấy được splash image: {}", e.getMessage());
            }

            video.setUploadedToDoodStreamAt(LocalDateTime.now());

            videoRepository.save(video);
            log.info("✅ [Streamtape - New Thread] Hoàn tất upload video {}! Status: {}, thumbnail: {}", videoId, video.getStatus(), video.getThumbnailUrl());

        } catch (Exception e) {
            log.error("❌ [Streamtape - New Thread] Upload thất bại cho video {}: {}", videoId, e.getMessage(), e);
            try {
                Video video = videoRepository.findById(videoId).orElse(null);
                if (video != null) {
                    video.setStatus(VideoStatus.FAILED);
                    videoRepository.save(video);
                }
            } catch (Exception ex) {
                log.error("❌ [Streamtape - New Thread] Không thể cập nhật status failed: {}", ex.getMessage());
            }
        } finally {
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (deleted) {
                    log.info("🗑️ [Streamtape] Đã xóa file tạm: {}", tempFilePath);
                } else {
                    log.warn("⚠️ [Streamtape] Không thể xóa file tạm: {}", tempFilePath);
                }
            }
        }
    }
}
