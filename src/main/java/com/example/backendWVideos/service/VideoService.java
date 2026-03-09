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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final RedisTemplate<String, String> redisTemplate;
    private final CategoryService categoryService;

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

        // Lấy categories nếu có (yêu cầu ít nhất 1, tối đa 10 categories)
        java.util.Set<com.example.backendWVideos.entity.Category> categories = new java.util.HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            if (request.getCategoryIds().size() < 1 || request.getCategoryIds().size() > 10) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            
            for (String categoryId : request.getCategoryIds()) {
                try {
                    categoryService.getCategoryById(categoryId);
                    com.example.backendWVideos.entity.Category category = new com.example.backendWVideos.entity.Category();
                    category.setId(categoryId);
                    categories.add(category);
                } catch (Exception e) {
                    log.warn("⚠️ Category không tồn tại: {}", categoryId);
                }
            }
            
            if (categories.size() < 1) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Tạo video record với status UPLOADING
        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .status(VideoStatus.UPLOADING)
                .user(user)
                .categories(categories)
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
        
        return videos.map(videoMapper::toVideoResponse);
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
        
        // Cập nhật categories (yêu cầu ít nhất 1, tối đa 10 categories)
        if (request.getCategoryIds() != null) {
            if (request.getCategoryIds().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            
            if (request.getCategoryIds().size() < 1 || request.getCategoryIds().size() > 10) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            
            java.util.Set<com.example.backendWVideos.entity.Category> categories = new java.util.HashSet<>();
            for (String categoryId : request.getCategoryIds()) {
                try {
                    categoryService.getCategoryById(categoryId);
                    com.example.backendWVideos.entity.Category category = new com.example.backendWVideos.entity.Category();
                    category.setId(categoryId);
                    categories.add(category);
                } catch (Exception e) {
                    log.warn("⚠️ Category không tồn tại khi update: {}", categoryId);
                }
            }
            
            if (categories.size() < 1) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            
            video.setCategories(categories);
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
    public void incrementViews(String videoId, String clientIp) {
        // Kiểm tra video có tồn tại không
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        
        // Rate limiting: chỉ cho phép tăng view từ cùng IP sau 5 phút
        String cacheKey = "view_" + videoId + "_" + clientIp;
        
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            log.info("⏰ Rate limit: IP {} đã xem video {} trong 5 phút qua", clientIp, videoId);
            return; // Không tăng view nếu đã xem trong 5 phút
        }
        
        // Sử dụng atomic update để tối ưu performance
        int updatedRows = videoRepository.incrementViewsById(videoId);
        
        if (updatedRows > 0) {
            // Lưu cache để rate limiting (5 phút)
            redisTemplate.opsForValue().set(cacheKey, "1", Duration.ofMinutes(5));
            
            log.info("👁️ Đã tăng lượt xem cho video: {} từ IP: {}", video.getTitle(), clientIp);
        }
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
