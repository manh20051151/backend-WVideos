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
        
        log.info("📋 Found {} videos for user {}", videos.getTotalElements(), userEmail);
        
        // Chỉ sync video cần thiết (PROCESSING hoặc chưa có thumbnail)
        videos.forEach(video -> {
            log.info("🔍 Checking video {}: status={}, fileCode={}, lastSync={}", 
                video.getId(), video.getStatus(), video.getFileCode(), video.getLastSyncedAt());
                
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
            log.debug("❌ Skip sync video {}: No fileCode", video.getId());
            return false;
        }
        
        // 1. Video đang PROCESSING hoặc UPLOADING - luôn sync
        if (video.getStatus() == VideoStatus.PROCESSING || video.getStatus() == VideoStatus.UPLOADING) {
            log.info("✅ Sync video {} - Status: {}", video.getId(), video.getStatus());
            return true;
        }
        
        // 2. Video chưa có splash image - cần sync để lấy thumbnail
        if (video.getSplashImageUrl() == null || video.getSplashImageUrl().isEmpty()) {
            log.debug("✅ Sync video {} - No splash image", video.getId());
            return true;
        }
        
        // 3. Chưa sync lần nào - cần sync
        if (video.getLastSyncedAt() == null) {
            log.debug("✅ Sync video {} - Never synced", video.getId());
            return true;
        }
        
        // 4. Đã sync nhưng quá 5 phút - sync lại để cập nhật views, status
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        boolean shouldSync = video.getLastSyncedAt().isBefore(fiveMinutesAgo);
        if (shouldSync) {
            log.info("✅ Sync video {} - Last sync: {}", video.getId(), video.getLastSyncedAt());
        } else {
            log.info("⏭️ Skip sync video {} - Recently synced: {}", video.getId(), video.getLastSyncedAt());
        }
        return shouldSync;
    }
    
    /**
     * Sync thông tin video từ DoodStream (silent mode - không throw exception)
     */
    private void syncVideoInfoSilent(Video video) {
        try {
            log.info("🔄 Syncing video {}: fileCode={}, status={}", 
                video.getId(), video.getFileCode(), video.getStatus());
                
            // Sử dụng file/list API thay vì file/info vì file/list cho kết quả chính xác hơn
            Map<String, Object> fileListResponse = doodStreamService.getFileList();
            
            if (fileListResponse != null && fileListResponse.get("result") != null) {
                Map<String, Object> listResult = (Map<String, Object>) fileListResponse.get("result");
                List<Map<String, Object>> files = (List<Map<String, Object>>) listResult.get("files");
                
                // Tìm file theo fileCode
                Map<String, Object> fileData = files.stream()
                    .filter(file -> video.getFileCode().equals(file.get("file_code")))
                    .findFirst()
                    .orElse(null);
                
                if (fileData != null) {
                    log.info("📊 DoodStream file list response for {}: canplay={}, views={}", 
                        video.getId(), fileData.get("canplay"), fileData.get("views"));
                    
                    // Cập nhật thông tin video từ file list
                    if (fileData.get("views") != null) {
                        video.setViews(Long.parseLong(fileData.get("views").toString()));
                    }
                    
                    // File list không có size, giữ nguyên size hiện tại
                    
                    if (fileData.get("length") != null) {
                        video.setDuration(Long.parseLong(fileData.get("length").toString()));
                    }
                    
                    if (fileData.get("canplay") != null) {
                        int canPlay = Integer.parseInt(fileData.get("canplay").toString());
                        VideoStatus oldStatus = video.getStatus();
                        VideoStatus newStatus = canPlay == 1 ? VideoStatus.READY : VideoStatus.PROCESSING;
                        video.setStatus(newStatus);
                        
                        if (oldStatus != newStatus) {
                            log.info("🔄 Status changed for video {}: {} -> {} (using file/list API)", 
                                video.getId(), oldStatus, newStatus);
                        }
                    }
                    
                    // Cập nhật thumbnails nếu có
                    if (fileData.get("single_img") != null) {
                        video.setThumbnailUrl(fileData.get("single_img").toString());
                    }
                    
                    if (fileData.get("splash_img") != null) {
                        video.setSplashImageUrl(fileData.get("splash_img").toString());
                    }
                    
                    // Cập nhật download URL từ file list
                    if (fileData.get("download_url") != null) {
                        video.setDownloadUrl(fileData.get("download_url").toString());
                    }
                    
                    // Cập nhật timestamp sync
                    video.setLastSyncedAt(LocalDateTime.now());
                    
                    videoRepository.save(video);
                    log.info("✅ Auto-sync video {}: {} views, status={} (via file/list)", 
                        video.getId(), video.getViews(), video.getStatus());
                } else {
                    log.warn("⚠️ File {} not found in DoodStream file list", video.getFileCode());
                }
            } else {
                log.warn("⚠️ No result from DoodStream file list");
            }
        } catch (Exception e) {
            log.warn("⚠️ Sync failed for video {}: {}", video.getId(), e.getMessage());
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
    
    /**
     * Test so sánh DoodStream APIs
     */
    public Map<String, Object> testDoodStreamAPIs(String fileCode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test file/info API
            Map<String, Object> fileInfo = doodStreamService.getFileInfo(fileCode);
            result.put("fileInfo", fileInfo);
            
            // Test file/list API
            Map<String, Object> fileList = doodStreamService.getFileList();
            result.put("fileList", fileList);
            
            // Tìm file trong list
            if (fileList.get("result") != null) {
                Map<String, Object> listResult = (Map<String, Object>) fileList.get("result");
                List<Map<String, Object>> files = (List<Map<String, Object>>) listResult.get("files");
                
                Map<String, Object> foundFile = files.stream()
                    .filter(file -> fileCode.equals(file.get("file_code")))
                    .findFirst()
                    .orElse(null);
                    
                result.put("foundInList", foundFile);
            }
            
            log.info("🧪 Test result for {}: fileInfo canplay={}, list canplay={}", 
                fileCode, 
                extractCanplay(fileInfo),
                extractCanplayFromList(result));
                
        } catch (Exception e) {
            log.error("❌ Error testing APIs: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    private Object extractCanplay(Map<String, Object> fileInfo) {
        if (fileInfo != null && fileInfo.get("result") != null) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) fileInfo.get("result");
            if (!results.isEmpty()) {
                return results.get(0).get("canplay");
            }
        }
        return null;
    }
    
    private Object extractCanplayFromList(Map<String, Object> testResult) {
        Map<String, Object> foundFile = (Map<String, Object>) testResult.get("foundInList");
        return foundFile != null ? foundFile.get("canplay") : null;
    }
}
