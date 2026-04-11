package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.VideoUpdateRequest;
import com.example.backendWVideos.dto.request.VideoUploadRequest;
import com.example.backendWVideos.dto.request.VideoInitUploadRequest;
import com.example.backendWVideos.dto.response.DoodStreamUploadResult;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.dto.response.VideoInitUploadResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.VideoStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.mapper.VideoMapper;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoRepository;
import com.example.backendWVideos.repository.SubscriptionRepository;
import com.example.backendWVideos.repository.VideoReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;

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
    private final VideoUploadAsyncService videoUploadAsyncService;
    private final SubscriptionRepository subscriptionRepository;
    private final VideoReactionRepository videoReactionRepository;

    /**
     * Init upload - Tạo video record và lấy upload server
     */
    @Transactional
    public VideoInitUploadResponse initUpload(String userEmail, VideoInitUploadRequest request) {
        log.info("🚀 === INIT UPLOAD === Bắt đầu init upload cho user: {}", userEmail);

        // Lấy user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        log.info("✅ User found: {}", user.getId());

        // Xử lý categories
        java.util.Set<com.example.backendWVideos.entity.Category> categories = processCategories(request.getCategoryIds());

        // Tạo video record với status UPLOADING
        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .status(VideoStatus.UPLOADING)
                .user(user)
                .categories(categories)
                .tags(request.getTags() != null ? request.getTags() : new java.util.HashSet<>())
                .thumbnailUrl(request.getThumbnailUrl())
                .build();

        video = videoRepository.save(video);
        log.info("📹 Tạo video record: {}", video.getId());

        // Lấy upload server từ DoodStream
        String uploadServerUrl = doodStreamService.getUploadServer();
        log.info("🌐 Upload server: {}", uploadServerUrl);

        // Tạo upload token (simple implementation)
        String uploadToken = java.util.UUID.randomUUID().toString();

        VideoInitUploadResponse response = new VideoInitUploadResponse();
        response.setVideoId(video.getId());
        response.setUploadServerUrl(uploadServerUrl);
        response.setUploadToken(uploadToken);
        response.setStatus("READY");

        log.info("✅ === INIT TRẢ VỀ === videoId: {}, uploadServer: {}", video.getId(), uploadServerUrl);
        return response;
    }

    /**
     * Complete upload - Cập nhật video với fileCode từ DoodStream
     */
    @Transactional
    public VideoResponse completeUpload(String userEmail, String videoId, String fileCode) {
        log.info("🏁 === COMPLETE UPLOAD === videoId: {}, fileCode: {}", videoId, fileCode);

        // Lấy user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lấy video
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!video.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Lấy thông tin file từ DoodStream
        Map<String, Object> fileInfo = doodStreamService.getFileInfo(fileCode);
        
        if (fileInfo != null && fileInfo.get("result") != null) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) fileInfo.get("result");
            
            if (!results.isEmpty()) {
                Map<String, Object> result = results.get(0);
                
                // Cập nhật thông tin video
                video.setFileCode(fileCode);
                video.setDownloadUrl(result.get("download_url") != null ? result.get("download_url").toString() : "https://dood.to/d/" + fileCode);
                video.setEmbedUrl("https://dood.to/e/" + fileCode);
                
                if (result.get("protected_dl") != null) {
                    video.setProtectedDownloadUrl(result.get("protected_dl").toString());
                }
                if (result.get("protected_embed") != null) {
                    video.setProtectedEmbedUrl(result.get("protected_embed").toString());
                }
                
                // Xử lý thumbnail - lưu domain gốc
                if (video.getThumbnailUrl() == null || video.getThumbnailUrl().isEmpty()) {
                    if (result.get("single_img") != null) {
                        video.setThumbnailUrl(result.get("single_img").toString());
                    }
                }
                
                // Splash image - lưu domain gốc
                if (result.get("splash_img") != null) {
                    video.setSplashImageUrl(result.get("splash_img").toString());
                }
                
                // Metadata
                if (result.get("size") != null) {
                    video.setFileSize(Long.parseLong(result.get("size").toString()));
                }
                if (result.get("length") != null) {
                    video.setDuration(Long.parseLong(result.get("length").toString()));
                }
                
                // Status
                if (result.get("canplay") != null) {
                    int canPlay = Integer.parseInt(result.get("canplay").toString());
                    video.setStatus(canPlay == 1 ? VideoStatus.READY : VideoStatus.PROCESSING);
                }
                
                video.setUploadedToDoodStreamAt(LocalDateTime.now());
                
                video = videoRepository.save(video);
                log.info("✅ Complete upload thành công: {}", videoId);
            }
        }

        return videoMapper.toVideoResponse(video);
    }

    /**
     * Complete upload by filename - Tìm file từ DoodStream dựa trên filename sau khi upload trực tiếp
     */
    @Transactional
    public VideoResponse completeUploadByFilename(String userEmail, String videoId, String filename) {
        log.info("🏁 === COMPLETE UPLOAD BY FILENAME === videoId: {}, filename: {}", videoId, filename);

        // Lấy user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lấy video
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!video.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Lấy danh sách file từ DoodStream
        Map<String, Object> fileList = doodStreamService.getFileList();
        
        if (fileList != null && fileList.get("result") != null) {
            Map<String, Object> listResult = (Map<String, Object>) fileList.get("result");
            List<Map<String, Object>> files = (List<Map<String, Object>>) listResult.get("files");
            
            // Tìm file vừa upload (dựa trên title/filename và thời gian gần nhất)
            String searchTitle = video.getTitle();
            Map<String, Object> foundFile = files.stream()
                    .filter(f -> {
                        String fileTitle = f.get("title") != null ? f.get("title").toString() : "";
                        String fileName = f.get("name") != null ? f.get("name").toString() : "";
                        return fileTitle.equalsIgnoreCase(searchTitle) || 
                               fileName.contains(filename) ||
                               filename.contains(fileName);
                    })
                    .findFirst()
                    .orElse(null);
            
            if (foundFile != null) {
                String fileCode = foundFile.get("file_code").toString();
                log.info("✅ Tìm thấy file trên DoodStream: fileCode={}", fileCode);
                
                // Cập nhật thông tin video
                video.setFileCode(fileCode);
                video.setDownloadUrl(foundFile.get("download_url") != null ? 
                    foundFile.get("download_url").toString() : "https://dood.to/d/" + fileCode);
                video.setEmbedUrl("https://dood.to/e/" + fileCode);
                
                if (foundFile.get("protected_dl") != null) {
                    video.setProtectedDownloadUrl(foundFile.get("protected_dl").toString());
                }
                if (foundFile.get("protected_embed") != null) {
                    video.setProtectedEmbedUrl(foundFile.get("protected_embed").toString());
                }
                
                // Xử lý thumbnail - lưu domain gốc
                if (video.getThumbnailUrl() == null || video.getThumbnailUrl().isEmpty()) {
                    if (foundFile.get("single_img") != null) {
                        video.setThumbnailUrl(foundFile.get("single_img").toString());
                    }
                }
                
                // Splash image - lưu domain gốc
                if (foundFile.get("splash_img") != null) {
                    video.setSplashImageUrl(foundFile.get("splash_img").toString());
                }
                
                // Metadata
                if (foundFile.get("size") != null) {
                    video.setFileSize(Long.parseLong(foundFile.get("size").toString()));
                }
                if (foundFile.get("length") != null) {
                    video.setDuration(Long.parseLong(foundFile.get("length").toString()));
                }
                
                // Status
                if (foundFile.get("canplay") != null) {
                    int canPlay = Integer.parseInt(foundFile.get("canplay").toString());
                    video.setStatus(canPlay == 1 ? VideoStatus.READY : VideoStatus.PROCESSING);
                }
                
                video.setUploadedToDoodStreamAt(LocalDateTime.now());
                
                video = videoRepository.save(video);
                log.info("✅ Complete upload by filename thành công: {}, fileCode: {}", videoId, fileCode);
            } else {
                log.warn("⚠️ Không tìm thấy file trên DoodStream với title: {}", searchTitle);
                // Vẫn trả về video nhưng status vẫn là UPLOADING
            }
        }

        return videoMapper.toVideoResponse(video);
    }

    /**
     * Upload video - Tạo record ngay, upload DoodStream async
     */
    @Transactional
    public VideoResponse uploadVideo(
            String userEmail,
            MultipartFile file,
            VideoUploadRequest request
    ) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 === ASYNC UPLOAD === Bắt đầu upload video cho user: {}", userEmail);
        
        // Validate file
        validateVideoFile(file);
        log.info("✅ File validation passed");

        // Lấy user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        log.info("✅ User found: {}", user.getId());

        // Xử lý categories
        java.util.Set<com.example.backendWVideos.entity.Category> categories = processCategories(request.getCategoryIds());

        // Tạo video record với status UPLOADING
        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .status(VideoStatus.UPLOADING)
                .user(user)
                .categories(categories)
                .tags(request.getTags() != null ? request.getTags() : new java.util.HashSet<>())
                .thumbnailUrl(request.getThumbnailUrl()) // Lưu thumbnail nếu có
                .build();

        video = videoRepository.save(video);
        long endTime = System.currentTimeMillis();
        log.info("📹 Tạo video record: {} sau {}ms - Bắt đầu async upload", video.getId(), (endTime - startTime));

        // Lưu file tạm trên disk để async method đọc (tránh giữ bytes trong memory)
        String tempFilePath = System.getProperty("java.io.tmpdir") + "/wvideos-uploads/" + video.getId() + "_" + file.getOriginalFilename();
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir") + "/wvideos-uploads/");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            file.transferTo(new File(tempFilePath));
            log.info("📦 Đã lưu file tạm: {} ({} bytes)", tempFilePath, file.getSize());
        } catch (Exception e) {
            log.error("❌ Lỗi khi lưu file tạm: {}", e.getMessage());
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }

        // Flush để commit transaction ngay lập tức, đảm bảo async method tìm thấy video
        videoRepository.flush();

        // Trigger async upload lên DoodStream - truyền file path thay vì bytes
        videoUploadAsyncService.uploadToDoodStreamAsync(video.getId(), tempFilePath, file.getOriginalFilename(), request.getThumbnailUrl());

        log.info("✅ === TRẢ VỀ NGAY === sau {}ms", (System.currentTimeMillis() - startTime));
        // Trả về ngay lập tức, không đợi upload DoodStream
        return videoMapper.toVideoResponse(video);
    }
    
    /**
     * Xử lý categories
     */
    private java.util.Set<com.example.backendWVideos.entity.Category> processCategories(java.util.List<String> categoryIds) {
        java.util.Set<com.example.backendWVideos.entity.Category> categories = new java.util.HashSet<>();
        
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        if (categoryIds.size() < 1 || categoryIds.size() > 10) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        log.info("🔄 Đang xử lý {} categories", categoryIds.size());
        
        for (String categoryId : categoryIds) {
            try {
                categoryService.getCategoryById(categoryId);
                com.example.backendWVideos.entity.Category category = new com.example.backendWVideos.entity.Category();
                category.setId(categoryId);
                categories.add(category);
                log.info("✅ Category {} hợp lệ", categoryId);
            } catch (Exception e) {
                log.warn("⚠️ Category không tồn tại: {}", categoryId);
            }
        }
        
        if (categories.size() < 1) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        log.info("✅ Đã xử lý xong {} categories", categories.size());
        return categories;
    }

    /**
     * Lấy danh sách video của user (không bao gồm DELETED)
     */
    @Transactional
    public Page<VideoResponse> getMyVideos(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        Page<Video> videos = videoRepository.findByUserIdAndStatusNot(user.getId(), VideoStatus.DELETED, pageable);
        
        log.info("📋 Found {} videos for user {}", videos.getTotalElements(), userEmail);
        
        return videos.map(videoMapper::toVideoResponse);
    }
    
    /**
     * Lấy danh sách video đã xóa (thùng rác)
     */
    @Transactional
    public Page<VideoResponse> getDeletedVideos(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        Page<Video> videos = videoRepository.findByUserIdAndStatus(user.getId(), VideoStatus.DELETED, pageable);
        
        log.info("�️ Found {} deleted videos for user {}", videos.getTotalElements(), userEmail);
        
        return videos.map(videoMapper::toVideoResponse);
    }
    
    /**
     * Khôi phục video đã xóa
     */
    @Transactional
    public VideoResponse restoreVideo(String userEmail, String videoId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        if (!video.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (video.getStatus() != VideoStatus.DELETED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        video.setStatus(VideoStatus.READY);
        video = videoRepository.save(video);
        
        log.info("♻️ Khôi phục video: {}", videoId);
        
        return videoMapper.toVideoResponse(video);
    }
    
    /**
     * Lấy danh sách video public với sort type - sử dụng native query để tối ưu performance
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getPublicVideos(Pageable pageable, String sortType) {
        String sort = sortType != null ? sortType : "newest";
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Video> videos;
        switch (sort) {
            case "popular":
                videos = videoRepository.findPublicVideosByViews(newPageable);
                break;
            case "favorites":
                videos = videoRepository.findPublicVideosByFavorites(newPageable);
                break;
            case "comments":
                videos = videoRepository.findPublicVideosByComments(newPageable);
                break;
            case "longest":
                videos = videoRepository.findPublicVideosByDuration(newPageable);
                break;
            case "newest":
            default:
                videos = videoRepository.findPublicVideosNative(newPageable);
                break;
        }
        
        return videos.map(videoMapper::toVideoResponse);
    }

    /**
     * Lấy danh sách tất cả video (bao gồm cả không công khai) - hiển thị cho tất cả mọi người
     * Chỉ khi click vào xem mới yêu cầu đăng nhập với video không công khai
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getAllVideos(Pageable pageable, String sortType) {
        String sort = sortType != null ? sortType : "newest";
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Video> videos;
        switch (sort) {
            case "popular":
                videos = videoRepository.findAllVideosByViews(newPageable);
                break;
            case "favorites":
                videos = videoRepository.findAllVideosByFavorites(newPageable);
                break;
            case "comments":
                videos = videoRepository.findAllVideosByComments(newPageable);
                break;
            case "longest":
                videos = videoRepository.findAllVideosByDuration(newPageable);
                break;
            case "newest":
            default:
                videos = videoRepository.findAllVideosByCreatedAt(newPageable);
                break;
        }
        
        return videos.map(videoMapper::toVideoResponse);
    }

    /**
     * Lấy chi tiết video - cho phép xem video công khai mà không cần đăng nhập,
     * và cho phép người dùng đã đăng nhập xem video không công khai
     */
    @Transactional(readOnly = true)
    public VideoResponse getVideoById(String videoId, String userEmail) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        
        // Nếu video không công khai và không có người dùng đăng nhập thì không cho xem
        if (!video.getIsPublic() && (userEmail == null || userEmail.isEmpty() || "anonymousUser".equals(userEmail))) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để xem video này");
        }
        
        VideoResponse response = videoMapper.toVideoResponse(video);
        
        // Lấy số người đăng ký của channel
        long subscriberCount = subscriptionRepository.countByChannelId(video.getUser().getId());
        response.setSubscriberCount(subscriberCount);
        
        // Kiểm tra user hiện tại đã đăng ký chưa
        if (userEmail != null && !userEmail.isEmpty() && !"anonymousUser".equals(userEmail)) {
            User currentUser = userRepository.findByEmail(userEmail).orElse(null);
            if (currentUser != null) {
                boolean isSubscribed = subscriptionRepository.existsBySubscriberIdAndChannelId(
                        currentUser.getId(), video.getUser().getId());
                response.setIsSubscribed(isSubscribed);
                
                // Lấy reaction của user
                var userReaction = videoReactionRepository.findByUserIdAndVideoId(currentUser.getId(), videoId);
                userReaction.ifPresent(r -> response.setUserReaction(r.getReactionType()));
            }
        }
        
        // Lấy số lượng reactions
        long likeCount = videoReactionRepository.countByVideoIdAndReactionType(videoId, com.example.backendWVideos.enums.VideoReactionType.LIKE);
        long dislikeCount = videoReactionRepository.countByVideoIdAndReactionType(videoId, com.example.backendWVideos.enums.VideoReactionType.DISLIKE);
        response.setLikeCount(likeCount);
        response.setDislikeCount(dislikeCount);
        
        return response;
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
        
        // Cập nhật thumbnail nếu có
        if (request.getThumbnailUrl() != null) {
            video.setThumbnailUrl(request.getThumbnailUrl());
            log.info("🖼️ Cập nhật thumbnail cho video: {}", videoId);
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
                    
                    // Cập nhật thumbnails nếu có - lưu domain gốc
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
