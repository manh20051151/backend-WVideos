package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.DoodStreamUploadResult;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.VideoRepository;
import com.example.backendWVideos.enums.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadAsyncService {

    private final VideoRepository videoRepository;
    private final DoodStreamService doodStreamService;
    
    // Thư mục lưu file tạm
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/wvideos-uploads/";
    
    static {
        // Tạo thư mục nếu chưa tồn tại
        File dir = new File(TEMP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void uploadToDoodStreamAsync(String videoId, String tempFilePath, String originalFilename, String customThumbnailUrl) {
        log.info("🔄 [Async - New Thread] Bắt đầu upload video {} lên DoodStream", videoId);
        
        File tempFile = null;
        try {
            // Đọc file từ disk
            tempFile = new File(tempFilePath);
            if (!tempFile.exists()) {
                throw new AppException(ErrorCode.UPLOAD_FAILED);
            }
            
            byte[] fileBytes = Files.readAllBytes(tempFile.toPath());
            log.info("📦 [Async] Đã đọc {} bytes từ file tạm: {}", fileBytes.length, tempFilePath);
            
            // Lấy video record
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
            
            // Lấy upload server
            String uploadServerUrl = doodStreamService.getUploadServer();
            log.info("🔄 [Async - New Thread] Upload server: {}", uploadServerUrl);

            // Upload file lên DoodStream
            DoodStreamUploadResult uploadResult = doodStreamService.uploadFileBytes(fileBytes, originalFilename, uploadServerUrl);
            log.info("✅ [Async - New Thread] Upload lên DoodStream thành công, fileCode: {}", uploadResult.getFileCode());

            // Cập nhật thông tin video
            video.setFileCode(uploadResult.getFileCode());
            video.setDownloadUrl(uploadResult.getDownloadUrl());
            video.setEmbedUrl("https://dood.to/e/" + uploadResult.getFileCode());
            video.setProtectedEmbedUrl(uploadResult.getProtectedEmbed());
            video.setProtectedDownloadUrl(uploadResult.getProtectedDl());

            // Xử lý thumbnail - lưu domain gốc
            if (customThumbnailUrl != null && !customThumbnailUrl.isEmpty()) {
                log.info("🖼️ [Async - New Thread] Sử dụng thumbnail tùy chỉnh: {}", customThumbnailUrl);
            } else if (uploadResult.getSingleImg() != null) {
                video.setThumbnailUrl(uploadResult.getSingleImg());
            }

            // Lưu splash image - lưu domain gốc
            if (uploadResult.getSplashImg() != null) {
                video.setSplashImageUrl(uploadResult.getSplashImg());
            }

            // Cập nhật metadata
            if (uploadResult.getSize() != null) {
                video.setFileSize(Long.parseLong(uploadResult.getSize()));
            }
            if (uploadResult.getLength() != null) {
                video.setDuration(Long.parseLong(uploadResult.getLength()));
            }

            // Cập nhật status
            video.setStatus(uploadResult.getCanPlay() == 1 ? VideoStatus.READY : VideoStatus.PROCESSING);
            video.setUploadedToDoodStreamAt(LocalDateTime.now());

            videoRepository.save(video);
            log.info("✅ [Async - New Thread] Hoàn tất upload video {}! Status: {}", videoId, video.getStatus());

        } catch (Exception e) {
            log.error("❌ [Async - New Thread] Upload thất bại cho video {}: {}", videoId, e.getMessage(), e);
            try {
                Video video = videoRepository.findById(videoId).orElse(null);
                if (video != null) {
                    video.setStatus(VideoStatus.FAILED);
                    videoRepository.save(video);
                }
            } catch (Exception ex) {
                log.error("❌ [Async - New Thread] Không thể cập nhật status failed: {}", ex.getMessage());
            }
        } finally {
            // Xóa file tạm sau khi upload (dù thành công hay thất bại)
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (deleted) {
                    log.info("🗑️ [Async] Đã xóa file tạm: {}", tempFilePath);
                } else {
                    log.warn("⚠️ [Async] Không thể xóa file tạm: {}", tempFilePath);
                }
            }
        }
    }
}
