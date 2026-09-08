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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.backendWVideos.entity.Category;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final DoodStreamService doodStreamService;
    private final StreamtapeService streamtapeService;
    private final VideoMapper videoMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final CategoryService categoryService;
    private final VideoUploadAsyncService videoUploadAsyncService;
    private final StreamtapeUploadAsyncService streamtapeUploadAsyncService;
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

        // Lấy upload URL từ Streamtape
        String uploadServerUrl = streamtapeService.getUploadUrl();
        log.info("🌐 Streamtape upload URL: {}", uploadServerUrl);

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

        // Lấy thông tin file từ Streamtape
        Map<String, Object> fileInfo = streamtapeService.getFileInfo(fileCode);

        if (fileInfo != null && fileInfo.get("result") != null) {
            Map<String, Object> resultMap = (Map<String, Object>) fileInfo.get("result");
            Map<String, Object> result = (Map<String, Object>) resultMap.get(fileCode);

            if (result != null) {
                // Cập nhật thông tin video
                video.setFileCode(fileCode);
                video.setEmbedUrl("https://streamtape.com/e/" + fileCode);

                // Metadata
                if (result.get("size") != null) {
                    video.setFileSize(Long.parseLong(result.get("size").toString()));
                }

                // Status dựa trên trạng thái converted của Streamtape
                boolean converted = result.get("converted") != null
                        && Boolean.parseBoolean(result.get("converted").toString());
                video.setStatus(converted ? VideoStatus.READY : VideoStatus.PROCESSING);

                // Thumbnail từ splash image (Streamtape không trả ảnh trong info)
                if (video.getThumbnailUrl() == null || video.getThumbnailUrl().isEmpty()) {
                    try {
                        String splash = streamtapeService.getSplashImage(fileCode);
                        if (splash != null && !splash.isEmpty()) {
                            video.setThumbnailUrl(splash);
                            video.setSplashImageUrl(splash);
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ Không lấy được splash image Streamtape: {}", e.getMessage());
                    }
                }

                video.setUploadedToDoodStreamAt(LocalDateTime.now());

                video = videoRepository.save(video);
                log.info("✅ Complete upload (Streamtape) thành công: {}", videoId);
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

        // Lấy danh sách file từ Streamtape
        Map<String, Object> fileList = streamtapeService.getFileList();

        if (fileList != null && fileList.get("result") != null) {
            Map<String, Object> listResult = (Map<String, Object>) fileList.get("result");
            List<Map<String, Object>> files = (List<Map<String, Object>>) listResult.get("files");

            // Tìm file vừa upload (dựa trên title/filename)
            String searchTitle = video.getTitle();
            Map<String, Object> foundFile = files.stream()
                    .filter(f -> {
                        String fileName = f.get("name") != null ? f.get("name").toString() : "";
                        return fileName.equalsIgnoreCase(searchTitle) ||
                               fileName.contains(filename) ||
                               filename.contains(fileName);
                    })
                    .findFirst()
                    .orElse(null);

            if (foundFile != null) {
                String fileCode = foundFile.get("linkid") != null
                        ? foundFile.get("linkid").toString()
                        : foundFile.get("file_code").toString();
                log.info("✅ Tìm thấy file trên Streamtape: fileCode={}", fileCode);

                // Cập nhật thông tin video
                video.setFileCode(fileCode);
                video.setEmbedUrl("https://streamtape.com/e/" + fileCode);

                // Metadata
                if (foundFile.get("size") != null) {
                    video.setFileSize(Long.parseLong(foundFile.get("size").toString()));
                }

                // Status dựa trên trạng thái convert (Streamtape: "converted" hoặc boolean)
                boolean converted;
                Object convertObj = foundFile.get("convert");
                if (convertObj instanceof Boolean) {
                    converted = (Boolean) convertObj;
                } else {
                    converted = convertObj != null && "converted".equalsIgnoreCase(convertObj.toString());
                }
                video.setStatus(converted ? VideoStatus.READY : VideoStatus.PROCESSING);

                // Thumbnail từ splash image (Streamtape không trả ảnh trong list)
                if (video.getThumbnailUrl() == null || video.getThumbnailUrl().isEmpty()) {
                    try {
                        String splash = streamtapeService.getSplashImage(fileCode);
                        if (splash != null && !splash.isEmpty()) {
                            video.setThumbnailUrl(splash);
                            video.setSplashImageUrl(splash);
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ Không lấy được splash image Streamtape: {}", e.getMessage());
                    }
                }

                video.setUploadedToDoodStreamAt(LocalDateTime.now());

                video = videoRepository.save(video);
                log.info("✅ Complete upload by filename (Streamtape) thành công: {}, fileCode: {}", videoId, fileCode);
            } else {
                log.warn("⚠️ Không tìm thấy file trên Streamtape với title: {}", searchTitle);
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

        // Flush đẩy SQL xuống DB (chưa commit). Việc đảm bảo async đọc được record
        // được xử lý bằng TransactionSynchronization.afterCommit() ở dưới.
        videoRepository.flush();

        // Chọn provider upload (mặc định Streamtape)
        com.example.backendWVideos.enums.VideoProvider provider =
                com.example.backendWVideos.enums.VideoProvider.fromValue(request.getProvider());
        video.setProvider(provider.getValue());

        // Lưu ID và các thông tin cần thiết vào biến final để dùng trong callback afterCommit
        final String videoId = video.getId();
        final String finalTempFilePath = tempFilePath;
        final String originalFilename = file.getOriginalFilename();
        final String customThumbnailUrl = request.getThumbnailUrl();
        final boolean isStreamtape = provider == com.example.backendWVideos.enums.VideoProvider.STREAMTAPE;

        // Đăng ký async upload chạy SAU KHI transaction commit thành công.
        // Nếu gọi trực tiếp ở đây, thread async có thể đọc DB trước khi record video được commit
        // -> dẫn đến lỗi "Không tìm thấy video" (race condition giữa các transaction).
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (isStreamtape) {
                    streamtapeUploadAsyncService.uploadToStreamtapeAsync(
                            videoId, finalTempFilePath, originalFilename, customThumbnailUrl);
                } else {
                    videoUploadAsyncService.uploadToDoodStreamAsync(
                            videoId, finalTempFilePath, originalFilename, customThumbnailUrl);
                }
            }
        });
        log.info("✅ === ĐÃ ĐĂNG KÝ ASYNC UPLOAD (chạy sau commit) === sau {}ms", (System.currentTimeMillis() - startTime));

        // Trả về ngay lập tức, không đợi upload provider
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
            // Lấy thông tin từ Streamtape
            Map<String, Object> fileInfo = streamtapeService.getFileInfo(video.getFileCode());

            if (fileInfo != null && fileInfo.get("result") != null) {
                Map<String, Object> resultMap = (Map<String, Object>) fileInfo.get("result");
                Map<String, Object> result = (Map<String, Object>) resultMap.get(video.getFileCode());

                if (result != null) {
                    // Cập nhật thông tin video
                    if (result.get("size") != null) {
                        video.setFileSize(Long.parseLong(result.get("size").toString()));
                    }

                    if (result.get("converted") != null) {
                        boolean converted = Boolean.parseBoolean(result.get("converted").toString());
                        video.setStatus(converted ? VideoStatus.READY : VideoStatus.PROCESSING);
                    }

                    // Cập nhật thumbnail từ splash image
                    // Luôn lưu splash image để hover card hiện ảnh Streamtape,
                    // chỉ ghi đè thumbnail_url khi chưa có (giữ nguyên thumbnail tùy chỉnh)
                    try {
                        String splash = streamtapeService.getSplashImage(video.getFileCode());
                        if (splash != null && !splash.isEmpty()) {
                            if (video.getThumbnailUrl() == null || video.getThumbnailUrl().isEmpty()) {
                                video.setThumbnailUrl(splash);
                            }
                            if (video.getSplashImageUrl() == null || video.getSplashImageUrl().isEmpty()) {
                                video.setSplashImageUrl(splash);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ Không lấy được splash image Streamtape: {}", e.getMessage());
                    }

                    // Cập nhật timestamp sync
                    video.setLastSyncedAt(LocalDateTime.now());

                    video = videoRepository.save(video);
                    log.info("✅ Sync thông tin video (Streamtape) thành công");
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

    /**
     * Lấy danh sách video liên quan
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getRelatedVideos(String currentVideoId, Pageable pageable) {
        Video currentVideo = videoRepository.findById(currentVideoId)
            .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        // Cho phép người dùng không đăng nhập xem video liên quan của video public
        if (currentVideo.getStatus() != VideoStatus.READY || !currentVideo.getIsPublic()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }

        List<String> categoryIds = currentVideo.getCategories().stream()
            .map(Category::getId)
            .collect(Collectors.toList());

        List<String> tags = new ArrayList<>(currentVideo.getTags());

        log.info("🔍 Tìm video liên quan cho video: {}, categories: {}, tags: {}",
                currentVideoId, categoryIds.size(), tags.size());

        // Nếu không có category hoặc tag, tìm theo cùng user
        if ((categoryIds.isEmpty() && tags.isEmpty())) {
            Page<Video> userVideos = videoRepository.findByUserIdAndStatusAndIsPublicTrue(currentVideo.getUser().getId(), VideoStatus.READY, pageable);
            log.info("📺 Tìm thấy {} video cùng user", userVideos.getTotalElements());
            return userVideos.map(videoMapper::toVideoResponse);
        }

        Page<Video> relatedVideos = videoRepository.findRelatedVideos(
            currentVideoId,
            currentVideo.getUser().getId(),
            categoryIds,
            tags,
            pageable);

        log.info("📺 Tìm thấy {} video liên quan", relatedVideos.getTotalElements());

        // Nếu không tìm thấy video liên quan, trả về video public mới nhất
        if (relatedVideos.getTotalElements() == 0) {
            log.info("📺 Không tìm thấy video liên quan, trả về video public mới nhất");
            return videoRepository.findPublicVideosNative(pageable)
                .map(videoMapper::toVideoResponse);
        }

        return relatedVideos.map(videoMapper::toVideoResponse);
    }
}
