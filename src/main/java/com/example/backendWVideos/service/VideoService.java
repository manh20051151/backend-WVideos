package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.VideoUpdateRequest;
import com.example.backendWVideos.dto.request.VideoUploadRequest;
import com.example.backendWVideos.dto.response.DoodStreamUploadResult;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.VideoStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.mapper.VideoMapper;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final DoodStreamService doodStreamService;
    private final VideoMapper videoMapper;

    /**
     * Upload video lên DoodStream
     */
    @Transactional
    public VideoResponse uploadVideo(
            String userEmail,
            MultipartFile file,
            VideoUploadRequest request
    ) {
        // Validate file
        validateVideoFile(file);

        // Lấy user bằng email (vì JWT subject là email)
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Tạo video record với status UPLOADING
        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .status(VideoStatus.UPLOADING)
                .user(user)
                .build();

        video = videoRepository.save(video);
        log.info("📹 Tạo video record: {}", video.getId());

        try {
            // Lấy upload server
            String uploadServerUrl = doodStreamService.getUploadServer();

            // Upload file
            DoodStreamUploadResult uploadResult = doodStreamService.uploadFile(file, uploadServerUrl);

            // Cập nhật video với thông tin từ DoodStream
            video.setFileCode(uploadResult.getFileCode());
            video.setDownloadUrl(uploadResult.getDownloadUrl());
            video.setEmbedUrl("https://dood.to/e/" + uploadResult.getFileCode());
            video.setProtectedEmbedUrl(uploadResult.getProtectedEmbed());
            video.setProtectedDownloadUrl(uploadResult.getProtectedDl());
            video.setThumbnailUrl(uploadResult.getSingleImg());
            video.setSplashImageUrl(uploadResult.getSplashImg());
            
            // Parse size và duration
            if (uploadResult.getSize() != null) {
                video.setFileSize(Long.parseLong(uploadResult.getSize()));
            }
            if (uploadResult.getLength() != null) {
                video.setDuration(Long.parseLong(uploadResult.getLength()));
            }

            // Cập nhật status
            video.setStatus(uploadResult.getCanPlay() == 1 ? VideoStatus.READY : VideoStatus.PROCESSING);
            video.setUploadedToDoodStreamAt(LocalDateTime.now());

            video = videoRepository.save(video);
            log.info("✅ Upload video thành công! ID: {}, FileCode: {}", video.getId(), video.getFileCode());

            return videoMapper.toVideoResponse(video);

        } catch (Exception e) {
            // Nếu upload thất bại, cập nhật status
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            
            log.error("❌ Upload video thất bại: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy danh sách video của user với thông tin realtime từ DoodStream
     */
    @Transactional
    public Page<VideoResponse> getMyVideos(String userEmail, Pageable pageable) {
        // Lấy user bằng email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        Page<Video> videos = videoRepository.findByUserId(user.getId(), pageable);
        
        // Chỉ sync video cần thiết (PROCESSING hoặc chưa có thumbnail)
        videos.forEach(video -> {
            if (shouldSyncVideo(video)) {
                try {
                    syncVideoInfoSilent(video);
                } catch (Exception e) {
                    log.warn("⚠️ Không thể sync video {}: {}", video.getId(), e.getMessage());
                }
            }
        });
        
        return videos.map(videoMapper::toVideoResponse);
    }
    
    /**
     * Kiểm tra xem video có cần sync không
     * 
     * Logic:
     * - Sync nếu video đang PROCESSING hoặc UPLOADING
     * - Sync nếu video chưa có splash image (thumbnail)
     * - Sync nếu chưa sync lần nào (lastSyncedAt == null)
     * - Sync nếu đã sync nhưng quá 5 phút (để cập nhật views, status mới)
     */
    private boolean shouldSyncVideo(Video video) {
        if (video.getFileCode() == null || video.getFileCode().isEmpty()) {
            return false;
        }
        
        // 1. Video đang PROCESSING hoặc UPLOADING - luôn sync
        if (video.getStatus() == VideoStatus.PROCESSING || video.getStatus() == VideoStatus.UPLOADING) {
            return true;
        }
        
        // 2. Video chưa có splash image - cần sync để lấy thumbnail
        if (video.getSplashImageUrl() == null || video.getSplashImageUrl().isEmpty()) {
            return true;
        }
        
        // 3. Chưa sync lần nào - cần sync
        if (video.getLastSyncedAt() == null) {
            return true;
        }
        
        // 4. Đã sync nhưng quá 5 phút - sync lại để cập nhật views, status
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return video.getLastSyncedAt().isBefore(fiveMinutesAgo);
    }
    
    /**
     * Sync thông tin video từ DoodStream (silent mode - không throw exception)
     */
    private void syncVideoInfoSilent(Video video) {
        try {
            Map<String, Object> fileInfo = doodStreamService.getFileInfo(video.getFileCode());
            
            if (fileInfo != null && fileInfo.get("result") != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) fileInfo.get("result");
                
                if (!results.isEmpty()) {
                    Map<String, Object> result = results.get(0);
                    
                    // Cập nhật thông tin video
                    if (result.get("views") != null) {
                        video.setViews(Long.parseLong(result.get("views").toString()));
                    }
                    
                    if (result.get("size") != null) {
                        video.setFileSize(Long.parseLong(result.get("size").toString()));
                    }
                    
                    if (result.get("length") != null) {
                        video.setDuration(Long.parseLong(result.get("length").toString()));
                    }
                    
                    if (result.get("canplay") != null) {
                        int canPlay = Integer.parseInt(result.get("canplay").toString());
                        video.setStatus(canPlay == 1 ? VideoStatus.READY : VideoStatus.PROCESSING);
                    }
                    
                    // Cập nhật thumbnails nếu có
                    if (result.get("single_img") != null) {
                        video.setThumbnailUrl(result.get("single_img").toString());
                    }
                    
                    if (result.get("splash_img") != null) {
                        video.setSplashImageUrl(result.get("splash_img").toString());
                    }
                    
                    // Cập nhật timestamp sync
                    video.setLastSyncedAt(LocalDateTime.now());
                    
                    videoRepository.save(video);
                    log.debug("✅ Auto-sync video {}: {} views, status={}", 
                        video.getId(), video.getViews(), video.getStatus());
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Skip sync for video {}: {}", video.getId(), e.getMessage());
            // Không throw exception, dùng data cũ trong database
        }
    }

    /**
     * Lấy danh sách video public
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getPublicVideos(Pageable pageable) {
        return videoRepository.findByStatusAndIsPublic(VideoStatus.READY, true, pageable)
                .map(videoMapper::toVideoResponse);
    }

    /**
     * Lấy chi tiết video
     */
    @Transactional(readOnly = true)
    public VideoResponse getVideoById(String videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        
        return videoMapper.toVideoResponse(video);
    }

    /**
     * Cập nhật thông tin video
     */
    @Transactional
    public VideoResponse updateVideo(String userEmail, String videoId, VideoUpdateRequest request) {
        // Lấy user bằng email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!video.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Cập nhật thông tin
        if (request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        if (request.getIsPublic() != null) {
            video.setIsPublic(request.getIsPublic());
        }

        video = videoRepository.save(video);
        log.info("✏️ Cập nhật video: {}", videoId);

        return videoMapper.toVideoResponse(video);
    }

    /**
     * Xóa video
     */
    @Transactional
    public void deleteVideo(String userEmail, String videoId) {
        // Lấy user bằng email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!video.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);
        
        log.info("🗑️ Xóa video: {}", videoId);
    }

    /**
     * Tăng view count
     */
    @Transactional
    public void incrementViews(String videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        video.setViews(video.getViews() + 1);
        videoRepository.save(video);
    }

    /**
     * Validate video file
     */
    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        // Kiểm tra định dạng file
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        // Kiểm tra kích thước file (max 2GB)
        long maxSize = 2L * 1024 * 1024 * 1024; // 2GB
        if (file.getSize() > maxSize) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
    }
    
    /**
     * Sync thông tin video từ DoodStream
     */
    @Transactional
    public VideoResponse syncVideoInfo(String userEmail, String videoId) {
        // Lấy user bằng email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!video.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        try {
            // Lấy thông tin từ DoodStream
            Map<String, Object> fileInfo = doodStreamService.getFileInfo(video.getFileCode());
            
            if (fileInfo != null && fileInfo.get("result") != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) fileInfo.get("result");
                
                if (!results.isEmpty()) {
                    Map<String, Object> result = results.get(0);
                    
                    // Cập nhật thông tin video
                    if (result.get("views") != null) {
                        video.setViews(Long.parseLong(result.get("views").toString()));
                    }
                    
                    if (result.get("size") != null) {
                        video.setFileSize(Long.parseLong(result.get("size").toString()));
                    }
                    
                    if (result.get("length") != null) {
                        video.setDuration(Long.parseLong(result.get("length").toString()));
                    }
                    
                    if (result.get("canplay") != null) {
                        int canPlay = Integer.parseInt(result.get("canplay").toString());
                        video.setStatus(canPlay == 1 ? VideoStatus.READY : VideoStatus.PROCESSING);
                    }
                    
                    // Cập nhật thumbnails nếu có
                    if (result.get("single_img") != null) {
                        video.setThumbnailUrl(result.get("single_img").toString());
                    }
                    
                    if (result.get("splash_img") != null) {
                        video.setSplashImageUrl(result.get("splash_img").toString());
                    }
                    
                    // Cập nhật timestamp sync
                    video.setLastSyncedAt(LocalDateTime.now());
                    
                    video = videoRepository.save(video);
                    log.info("✅ Sync thông tin video thành công: {} views", video.getViews());
                }
            }
            
            return videoMapper.toVideoResponse(video);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi sync thông tin video: {}", e.getMessage());
            throw new AppException(ErrorCode.DOODSTREAM_ERROR);
        }
    }
}
