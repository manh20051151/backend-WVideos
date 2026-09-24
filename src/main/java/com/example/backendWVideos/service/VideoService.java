package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.VideoUpdateRequest;
import com.example.backendWVideos.dto.request.VideoUploadRequest;
import com.example.backendWVideos.dto.request.VideoInitUploadRequest;
import com.example.backendWVideos.dto.response.DoodStreamUploadResult;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.dto.response.VideoInitUploadResponse;
import com.example.backendWVideos.dto.response.ShortsResponse;
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
import com.example.backendWVideos.repository.WatchedVideoRepository;
import com.example.backendWVideos.repository.VideoPurchaseRepository;
import com.example.backendWVideos.repository.VideoViewLogRepository;
import com.example.backendWVideos.service.VideoViewLogService;
import com.example.backendWVideos.entity.WatchedVideo;
import com.example.backendWVideos.entity.VideoPurchase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.example.backendWVideos.entity.Category;

import org.springframework.beans.factory.annotation.Value;
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
    private final WatchedVideoRepository watchedVideoRepository;
    private final VideoPurchaseRepository videoPurchaseRepository;
    private final UserFinancialService userFinancialService;
    private final NotificationService notificationService;
    private final VideoViewLogRepository videoViewLogRepository;
    private final VideoViewLogService videoViewLogService;

    @Value("${app.revenue.creator-share-percent:70}")
    private double creatorSharePercent;

    /**
     * Tạo slug từ tiêu đề video (bỏ dấu tiếng Việt, viết thường, dấu gạch ngang)
     */
    private String generateSlug(String title) {
        String slug = com.example.backendWVideos.util.SlugUtils.slugify(title);
        if (slug.isEmpty()) {
            slug = "video-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }

    /**
     * Tạo slug duy nhất (thêm hậu tố -2, -3... nếu trùng)
     */
    private String generateUniqueSlug(String title) {
        String baseSlug = generateSlug(title);
        String slug = baseSlug;
        int counter = 2;
        while (videoRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    /**
     * Tìm video theo ID hoặc slug (hỗ trợ link /watch/{slug} và /watch/{id})
     */
    private Video findVideoByIdOrSlug(String idOrSlug) {
        return videoRepository.findById(idOrSlug)
                .or(() -> videoRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
    }

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
                .slug(generateUniqueSlug(request.getTitle()))
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .status(VideoStatus.UPLOADING)
                .user(user)
                .categories(categories)
                .tags(request.getTags() != null ? request.getTags() : new java.util.HashSet<>())
                .thumbnailUrl(request.getThumbnailUrl())
                .duration(request.getDuration()) // Thời lượng do frontend đọc từ metadata
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
                .slug(generateUniqueSlug(request.getTitle()))
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .status(VideoStatus.UPLOADING)
                .user(user)
                .categories(categories)
                .tags(request.getTags() != null ? request.getTags() : new java.util.HashSet<>())
                .thumbnailUrl(request.getThumbnailUrl()) // Lưu thumbnail nếu có
                .duration(request.getDuration()) // Thời lượng do frontend đọc từ metadata
                .price(request.getPrice() != null ? request.getPrice() : 0L) // Giá video, 0 = miễn phí
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
     * Lấy danh sách video đã mua của user (sắp xếp theo thời gian mua mới nhất)
     */
    @Transactional
    public Page<VideoResponse> getPurchasedVideos(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Page<VideoPurchase> purchases = videoPurchaseRepository.findByUserIdOrderByPurchasedAtDesc(user.getId(), pageable);

        List<String> videoIds = purchases.getContent().stream()
                .map(VideoPurchase::getVideoId)
                .toList();

        List<Video> videos = videoIds.isEmpty()
                ? List.of()
                : videoRepository.findAllById(videoIds);

        Map<String, Video> videoMap = videos.stream()
                .collect(Collectors.toMap(Video::getId, v -> v));

        List<VideoResponse> responses = purchases.getContent().stream()
                .map(p -> videoMap.get(p.getVideoId()))
                .filter(Objects::nonNull)
                .map(v -> {
                    VideoResponse response = videoMapper.toVideoResponse(v);
                    response.setIsPurchased(true);
                    response.setHasAccess(true);
                    return response;
                })
                .toList();

        log.info("🛒 Found {} purchased videos for user {}", purchases.getTotalElements(), userEmail);

        return new PageImpl<>(responses, pageable, purchases.getTotalElements());
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
     * Ẩn thông tin phát video (fileCode, embedUrl, ...) trong danh sách công khai
     * để tránh lộ mã nguồn video có phí qua các endpoint listing.
     */
    private void stripStreamInfo(VideoResponse r) {
        r.setFileCode(null);
        r.setEmbedUrl(null);
        r.setProtectedEmbedUrl(null);
        r.setDownloadUrl(null);
        r.setProtectedDownloadUrl(null);
    }

    /**
     * Lấy danh sách video public với sort type - sử dụng native query để tối ưu performance
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getPublicVideos(Pageable pageable, String sortType) {
        return getPublicVideos(pageable, sortType, null, null);
    }

    /**
     * Lấy video công khai, có thể lọc theo slug category (dropdown Thể loại, trang /category/{slug})
     * hoặc theo tag (trang /tag/{tag}).
     * readOnly: giữ Session mở suốt quá trình map response (entity graph chỉ fetch
     * user/tags của video, không fetch user của category — mapper cần lazy load).
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getPublicVideos(Pageable pageable, String sortType, String categorySlug, String tag) {
        String sort = sortType != null ? sortType : "newest";
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        // Map sort -> field entity, dùng cho cả lọc category lẫn tag
        String sortField = switch (sort) {
            case "popular" -> "views";
            case "favorites" -> "favoritesCount";
            case "comments" -> "commentsCount";
            case "longest" -> "duration";
            default -> "createdAt";
        };
        org.springframework.data.domain.Pageable sortedPageable = PageRequest.of(
                newPageable.getPageNumber(), newPageable.getPageSize(),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, sortField));

        Page<Video> videos;
        if (categorySlug != null && !categorySlug.isBlank()) {
            // Lọc theo category
            videos = videoRepository.findPublicVideosByCategorySlug(categorySlug.trim().toLowerCase(), sortedPageable);
        } else if (tag != null && !tag.isBlank()) {
            // Lọc theo tag
            videos = videoRepository.findPublicVideosByTag(tag.trim().toLowerCase(), sortedPageable);
        } else {
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
        }
        
        return videos.map(v -> {
            VideoResponse r = videoMapper.toVideoResponse(v);
            stripStreamInfo(r);
            return r;
        });
    }

    /**
     * Lấy video trending: lượt xem nhiều nhất trong khoảng thời gian (hours) gần nhất.
     * Dữ liệu đếm từ bảng video_view_logs, kết quả Top-N được cache 5 phút trên Redis.
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getTrendingVideos(int hours, Pageable pageable) {
        int size = pageable.getPageSize();
        int page = pageable.getPageNumber();

        String idsKey = "trending:ids:" + hours + ":" + page + ":" + size;
        String totalKey = "trending:total:" + hours;

        List<String> cachedIds = parseTrendingIds(redisTemplate.opsForValue().get(idsKey));
        String rawTotal = redisTemplate.opsForValue().get(totalKey);
        Long total = rawTotal != null ? Long.parseLong(rawTotal) : null;

        List<String> ids;
        if (cachedIds == null) {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            ids = videoViewLogRepository.findTrendingVideoIds(since, pageable);
            // Kết quả rỗng chỉ cache ngắn (60s) để video vừa xem hiện ra nhanh;
            // kết quả có data cache 5 phút để giảm tải DB.
            if (ids.isEmpty()) {
                redisTemplate.opsForValue().set(idsKey, "EMPTY", Duration.ofSeconds(60));
            } else {
                redisTemplate.opsForValue().set(idsKey, String.join(",", ids), Duration.ofMinutes(5));
            }
        } else {
            ids = cachedIds;
        }

        if (total == null) {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            total = videoViewLogRepository.countTrendingVideoIds(since);
            redisTemplate.opsForValue().set(
                    totalKey,
                    String.valueOf(total),
                    ids.isEmpty() ? Duration.ofSeconds(60) : Duration.ofMinutes(5)
            );
        }

        // Danh sách có thể thay đổi -> dùng List mutable để ghép thêm
        List<String> resultIds = new ArrayList<>(ids);

        // Nếu chưa đủ số lượng yêu cầu (vd <8) và ở trang đầu,
        // ghép thêm video xem nhiều nhất (all-time) để lấp đầy slot, không trùng lặp
        if (page == 0 && resultIds.size() < size) {
            List<String> popularIds = videoRepository.findAllVideosByViews(PageRequest.of(0, size))
                    .stream()
                    .map(Video::getId)
                    .toList();
            for (String pid : popularIds) {
                if (resultIds.size() >= size) break;
                if (!resultIds.contains(pid)) resultIds.add(pid);
            }
        }

        List<Video> videos = resultIds.isEmpty() ? List.of() : videoRepository.findAllById(resultIds);
        Map<String, Video> map = videos.stream().collect(Collectors.toMap(Video::getId, v -> v));
        List<VideoResponse> content = resultIds.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .map(v -> {
                    VideoResponse r = videoMapper.toVideoResponse(v);
                    stripStreamInfo(r);
                    return r;
                })
                .toList();

        // Trang đầu: tổng = số lượng thực tế đã ghép; trang sau: dùng tổng 24h
        long finalTotal = (page == 0) ? resultIds.size() : (total != null ? total : resultIds.size());
        return new PageImpl<>(content, pageable, finalTotal);
    }

    private List<String> parseTrendingIds(String raw) {
        if (raw == null) return null;
        if ("EMPTY".equals(raw)) return List.of();
        return Arrays.stream(raw.split(","))
                .filter(s -> !s.isEmpty())
                .toList();
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
        
        return videos.map(v -> {
            VideoResponse r = videoMapper.toVideoResponse(v);
            stripStreamInfo(r);
            return r;
        });
    }

    /**
     * Lấy chi tiết video - cho phép xem video công khai mà không cần đăng nhập,
     * và cho phép người dùng đã đăng nhập xem video không công khai
     */
    @Transactional(readOnly = true)
    public VideoResponse getVideoById(String videoId, String userEmail) {
        Video video = findVideoByIdOrSlug(videoId);
        
        // Nếu video không công khai và không có người dùng đăng nhập thì không cho xem
        if (!video.getIsPublic() && (userEmail == null || userEmail.isEmpty() || "anonymousUser".equals(userEmail))) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để xem video này");
        }
        
        VideoResponse response = videoMapper.toVideoResponse(video);

        // Lấy số người đăng ký của channel
        long subscriberCount = subscriptionRepository.countByChannelId(video.getUser().getId());
        response.setSubscriberCount(subscriberCount);

        String currentUserId = null;

        // Kiểm tra user hiện tại đã đăng ký chưa
        if (userEmail != null && !userEmail.isEmpty() && !"anonymousUser".equals(userEmail)) {
            User currentUser = userRepository.findByEmail(userEmail).orElse(null);
            if (currentUser != null) {
                currentUserId = currentUser.getId();
                boolean isSubscribed = subscriptionRepository.existsBySubscriberIdAndChannelId(
                        currentUser.getId(), video.getUser().getId());
                response.setIsSubscribed(isSubscribed);
                
                // Lấy reaction của user (lấy bản ghi đầu tiên nếu tồn tại trùng lặp)
                var userReactions = videoReactionRepository.findByUserIdAndVideoId(currentUser.getId(), video.getId());
                userReactions.stream().findFirst().ifPresent(r -> response.setUserReaction(r.getReactionType()));
            }
        }
        
        // Lấy số lượng reactions
        long likeCount = videoReactionRepository.countByVideoIdAndReactionType(video.getId(), com.example.backendWVideos.enums.VideoReactionType.LIKE);
        long dislikeCount = videoReactionRepository.countByVideoIdAndReactionType(video.getId(), com.example.backendWVideos.enums.VideoReactionType.DISLIKE);
        response.setLikeCount(likeCount);
        response.setDislikeCount(dislikeCount);

        // Kiểm tra quyền xem video có phí
        Long price = video.getPrice() != null ? video.getPrice() : 0L;
        boolean isOwner = currentUserId != null && video.getUser() != null
                && currentUserId.equals(video.getUser().getId());
        boolean purchased = currentUserId != null
                && videoPurchaseRepository.existsByUserIdAndVideoId(currentUserId, video.getId());
        boolean hasAccess = price == 0 || isOwner || purchased;

        response.setIsPurchased(purchased);
        response.setHasAccess(hasAccess);

        // Nếu không có quyền xem (video có phí chưa mua) thì không trả thông tin phát video
        if (!hasAccess) {
            response.setEmbedUrl(null);
            response.setProtectedEmbedUrl(null);
            response.setDownloadUrl(null);
            response.setProtectedDownloadUrl(null);
        }

        return response;
    }

    /**
     * Mua video có phí: trừ tiền từ ví người mua, cộng doanh thu cho chủ video.
     */
    @Transactional
    public VideoResponse purchaseVideo(String userEmail, String videoId) {
        if (userEmail == null || userEmail.isEmpty() || "anonymousUser".equals(userEmail)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để mua video");
        }

        User buyer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Video video = findVideoByIdOrSlug(videoId);

        Long price = video.getPrice() != null ? video.getPrice() : 0L;
        if (price == 0) {
            throw new AppException(ErrorCode.VIDEO_IS_FREE);
        }

        // Không cho mua video của chính mình
        if (video.getUser() != null && buyer.getId().equals(video.getUser().getId())) {
            throw new AppException(ErrorCode.CANNOT_PURCHASE_OWN_VIDEO);
        }

        // Đã mua rồi thì không mua lại
        if (videoPurchaseRepository.existsByUserIdAndVideoId(buyer.getId(), video.getId())) {
            throw new AppException(ErrorCode.VIDEO_ALREADY_PURCHASED);
        }

        // Trừ tiền người mua (nếu không đủ sẽ ném INSUFFICIENT_BALANCE)
        userFinancialService.updateUserBalance(buyer.getId(), price.doubleValue(), "SUBTRACT");

        // Lưu giao dịch mua video (lưu id thật, không phải slug)
        videoPurchaseRepository.save(VideoPurchase.builder()
                .userId(buyer.getId())
                .videoId(video.getId())
                .price(price)
                .build());

        // Cộng doanh thu cho chủ video (theo tỷ lệ chia sẻ cấu hình)
        if (video.getUser() != null) {
            double creatorRevenue = price.doubleValue() * creatorSharePercent / 100.0;
            userFinancialService.updateUserRevenue(video.getUser().getId(), creatorRevenue);

            // Thông báo realtime cho chủ video
            notificationService.notifyVideoPurchased(
                    video.getUser().getId(),
                    buyer.getId(),
                    buyer.getFullName(),
                    video.getId(),
                    video.getTitle(),
                    creatorRevenue,
                    video.getThumbnailUrl() != null ? video.getThumbnailUrl() : video.getSplashImageUrl(),
                    buyer.getAvatar()
            );
        }

        log.info("✅ User {} đã mua video {} với giá {}", buyer.getId(), videoId, price);

        // Trả về chi tiết video (đã có quyền xem)
        return getVideoById(videoId, userEmail);
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
        // Lưu ý: slug giữ nguyên khi đổi tiêu đề (giống YouTube) để link đã chia sẻ không bị vỡ
        if (request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        if (request.getIsPublic() != null) {
            video.setIsPublic(request.getIsPublic());
        }

        // Cập nhật giá video (0 = miễn phí, null = giữ nguyên giá hiện tại)
        if (request.getPrice() != null) {
            if (request.getPrice() < 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            video.setPrice(request.getPrice());
        }

        // Video có phí bắt buộc phải công khai (đồng bộ quy tắc với trang upload)
        if (video.getPrice() != null && video.getPrice() > 0) {
            video.setIsPublic(true);
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

        // Cập nhật tags (gửi mảng rỗng = xóa hết tags)
        if (request.getTags() != null) {
            video.setTags(request.getTags());
            log.info("🏷️ Cập nhật tags cho video: {}", videoId);
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
        // Hỗ trợ cả ID lẫn slug trên URL
        Video video = findVideoByIdOrSlug(videoId);
        videoId = video.getId();

        // Rate limiting: chỉ cho phép tăng view từ cùng IP sau 5 phút
        String cacheKey = "view_" + videoId + "_" + clientIp;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            log.info("⏰ Rate limit: IP {} đã xem video {} trong 5 phút qua", clientIp, videoId);
            return; // Không tăng view nếu đã xem trong 5 phút
        }

        // Tăng view bằng bulk update atomic (COALESCE tránh NULL),
        // không load entity vào persistence context để tránh bị ghi đè về 0 khi flush
        int updatedRows = videoRepository.incrementViewsById(videoId);

        if (updatedRows > 0) {
            // Lưu cache để rate limiting (5 phút)
            redisTemplate.opsForValue().set(cacheKey, "1", Duration.ofMinutes(5));

            // Ghi log lượt xem (bất đồng bộ) để tính trending 24h
            videoViewLogService.logView(videoId, clientIp);

            log.info("👁️ Đã tăng lượt xem cho video: {} từ IP: {}", videoId, clientIp);
        }
    }

    /**
     * Đánh dấu video đã xem (dùng cho shorts feed, không hiện lại video đã xem).
     * Chỉ đánh dấu khi user CÓ QUYỀN XEM video (miễn phí công khai, đã mua hoặc đã đăng nhập).
     * Nếu đánh dấu cả video không xem được thì guest scroll qua video riêng tư/video có phí
     * sẽ khiến video đó biến mất vĩnh viễn khỏi feed dù user chưa xem bao giờ.
     * Chạy async + idempotent (unique user_id+video_id) nên an toàn khi gọi nhiều lần.
     */
    @Async
    @Transactional
    public void markWatched(String userId, String videoId) {
        if (userId == null || userId.isBlank() || videoId == null || videoId.isBlank()) {
            return;
        }
        try {
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null) {
                return;
            }

            // Kiểm tra quyền xem: video có phí phải đã mua/là chủ, video riêng tư phải đăng nhập
            boolean paid = video.getPrice() != null && video.getPrice() > 0;
            boolean isPublic = video.getIsPublic() == null || video.getIsPublic();
            boolean hasUser = userId != null && !userId.isBlank();
            // Lấy userId đăng nhập thật (guestId không tính là đăng nhập)
            String authUserId = resolveAuthUserId();
            boolean authenticated = authUserId != null && !authUserId.isBlank();
            boolean isOwner = authenticated && video.getUser() != null
                    && authUserId.equals(video.getUser().getId());
            boolean purchased = paid && authenticated
                    && videoPurchaseRepository.existsByUserIdAndVideoId(authUserId, video.getId());
            boolean canStream = (!paid || purchased || isOwner) && (isPublic || authenticated);
            if (!canStream) {
                log.debug("⛔ Bỏ qua đánh dấu đã xem (user chưa có quyền xem video): {}", videoId);
                return;
            }

            if (watchedVideoRepository.existsByUserIdAndVideoId(userId, videoId)) {
                return;
            }
            watchedVideoRepository.save(WatchedVideo.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .watchedAt(LocalDateTime.now())
                    .build());
            log.info("✅ Đã đánh dấu đã xem: user={}, video={}", userId, videoId);
        } catch (DataIntegrityViolationException e) {
            // Trùng lặp (concurrent) -> bỏ qua
            log.debug("Video đã được đánh dấu xem trước đó: {}", videoId);
        }
    }

    /**
     * Lấy userId của user đã đăng nhập từ SecurityContext (không fallback guestId).
     * Dùng để kiểm tra quyền: video riêng tư cần đăng nhập, video có phí cần đã mua.
     */
    private String resolveAuthUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return null;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        return user != null ? user.getId() : null;
    }

    /**
     * Lấy feed shorts: danh sách TẤT CẢ video READY dưới 120s (kể cả video có phí và riêng tư),
     * loại trừ những video user đã xem, kèm theo streamUrl (direct mp4) đã resolve từ cache Redis.
     * - Video có phí: streamUrl null trừ khi đã mua hoặc là chủ video (frontend hiện "phải mua")
     * - Video riêng tư: streamUrl null nếu chưa đăng nhập (frontend hiện "phải đăng nhập")
     * Dùng keyset pagination theo createdAt để tránh OFFSET sâu.
     */
    @Transactional(readOnly = true)
    public List<ShortsResponse> getShorts(String userId, String authUserId, LocalDateTime lastCreatedAt, int size, boolean loop) {
        int limit = Math.min(Math.max(size, 1), 30);
        Pageable pageable = PageRequest.of(0, limit);

        List<Video> videos;
        if (loop) {
            // Chế độ lặp vô hạn: không loại trừ video đã xem để feed quay vòng
            videos = videoRepository.findShorts(lastCreatedAt, pageable);
        } else if (userId != null && !userId.isBlank()) {
            videos = videoRepository.findShortsExcludingWatched(userId, lastCreatedAt, pageable);
        } else {
            videos = videoRepository.findShorts(lastCreatedAt, pageable);
        }

        // Resolve streamUrl song song (dựa vào Redis cache nên lần 2 rất nhanh)
        return videos.parallelStream()
                .map(v -> toShortsResponse(v, authUserId))
                .collect(Collectors.toList());
    }

    private ShortsResponse toShortsResponse(Video video, String authUserId) {
        boolean paid = video.getPrice() != null && video.getPrice() > 0;
        boolean isPublic = video.getIsPublic() == null || video.getIsPublic();
        boolean hasUser = authUserId != null && !authUserId.isBlank();
        boolean purchased = false;
        boolean isOwner = false;
        if (hasUser && video.getUser() != null) {
            isOwner = authUserId.equals(video.getUser().getId());
            if (paid) {
                purchased = videoPurchaseRepository.existsByUserIdAndVideoId(authUserId, video.getId());
            }
        }
        // Video riêng tư: phải đăng nhập mới xem được
        boolean requireLogin = !isPublic && !hasUser;
        // Bảo mật: chỉ trả streamUrl khi được phép xem:
        // - Video có phí: phải đã mua hoặc là chủ video
        // - Video riêng tư: phải đăng nhập
        boolean canStream = (!paid || purchased || isOwner) && (isPublic || hasUser);
        return ShortsResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .slug(video.getSlug())
                .streamUrl(canStream ? resolveStreamUrl(video) : null)
                .thumbnailUrl(video.getThumbnailUrl())
                .splashImageUrl(video.getSplashImageUrl())
                .userFullName(video.getUser() != null ? video.getUser().getFullName() : null)
                .userId(video.getUser() != null ? video.getUser().getId() : null)
                .avatarUrl(video.getUser() != null ? video.getUser().getAvatar() : null)
                .duration(video.getDuration())
                .views(video.getViews())
                .likeCount(videoReactionRepository.countByVideoIdAndReactionType(
                        video.getId(), com.example.backendWVideos.enums.VideoReactionType.LIKE))
                .isLiked(hasUser && videoReactionRepository.existsByUserIdAndVideoIdAndReactionType(
                        authUserId, video.getId(), com.example.backendWVideos.enums.VideoReactionType.LIKE))
                .price(video.getPrice())
                .isPaid(paid)
                .purchased(purchased)
                .isOwner(isOwner)
                .isPublic(isPublic)
                .requireLogin(requireLogin)
                .createdAt(video.getCreatedAt())
                .build();
    }

    /**
     * Resolve direct mp4 URL theo provider. Streamtape dùng cache Redis (TTL 2h),
     * DoodStream scrape pass_md5. Trả null nếu không resolve được.
     */
    private String resolveStreamUrl(Video video) {
        if (video.getFileCode() == null || video.getFileCode().isBlank()) {
            return null;
        }
        try {
            if ("streamtape".equalsIgnoreCase(video.getProvider())) {
                return streamtapeService.getDirectVideoUrl(video.getFileCode());
            }
            if ("doodstream".equalsIgnoreCase(video.getProvider())) {
                return doodStreamService.getDirectVideoUrl(video.getFileCode());
            }
        } catch (Exception e) {
            log.warn("⚠️ Không resolve được streamUrl cho video {}: {}", video.getId(), e.getMessage());
        }
        return null;
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
        Video currentVideo = findVideoByIdOrSlug(currentVideoId);

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
            return userVideos.map(v -> {
                VideoResponse r = videoMapper.toVideoResponse(v);
                stripStreamInfo(r);
                return r;
            });
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
                .map(v -> {
                    VideoResponse r = videoMapper.toVideoResponse(v);
                    stripStreamInfo(r);
                    return r;
                });
        }

        return relatedVideos.map(v -> {
            VideoResponse r = videoMapper.toVideoResponse(v);
            stripStreamInfo(r);
            return r;
        });
    }
}
