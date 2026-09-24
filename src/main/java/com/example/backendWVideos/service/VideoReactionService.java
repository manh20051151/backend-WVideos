package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.VideoReactionResponse;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.entity.VideoReaction;
import com.example.backendWVideos.enums.VideoReactionType;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.mapper.VideoMapper;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoReactionRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoReactionService {
    
    private final VideoReactionRepository videoReactionRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VideoMapper videoMapper;
    private final NotificationService notificationService;
    
    // Toggle reaction (like/dislike)
    @Transactional
    public ApiResponse<VideoReactionResponse> toggleReaction(String videoId, VideoReactionType reactionType) {
        User user = getCurrentUser();
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        
        // Không thể tự react video của mình
        if (video.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.INVALID_DATA);
        }
        
        // Kiểm tra reaction hiện tại (có thể tồn tại bản ghi trùng do dữ liệu cũ)
        List<VideoReaction> existingReactions = videoReactionRepository.findByUserIdAndVideoId(user.getId(), videoId);
        // Dọn dẹp bản ghi trùng lặp: giữ 1, xóa các bản còn lại
        if (existingReactions.size() > 1) {
            for (int i = 1; i < existingReactions.size(); i++) {
                videoReactionRepository.delete(existingReactions.get(i));
            }
        }
        Optional<VideoReaction> existingReaction = existingReactions.stream().findFirst();
        
        VideoReaction reaction;
        boolean isNewReaction = false;
        
        if (existingReaction.isPresent()) {
            reaction = existingReaction.get();
            // Nếu click cùng loại -> xóa reaction (unlike/un-dislike)
            if (reaction.getReactionType() == reactionType) {
                videoReactionRepository.delete(reaction);
                log.info("User {} removed {} on video {}", user.getEmail(), reactionType, videoId);
            } else {
                // Click khác loại -> đổi reaction (like -> dislike hoặc ngược lại)
                reaction.setReactionType(reactionType);
                reaction = videoReactionRepository.save(reaction);
                log.info("User {} changed reaction to {} on video {}", user.getEmail(), reactionType, videoId);
            }
        } else {
            // Chưa có reaction -> tạo mới
            reaction = VideoReaction.builder()
                    .user(user)
                    .video(video)
                    .reactionType(reactionType)
                    .build();
            reaction = videoReactionRepository.save(reaction);
            isNewReaction = true;
            log.info("User {} added {} on video {}", user.getEmail(), reactionType, videoId);

            // Thông báo realtime cho chủ video khi có lượt thích mới
            if (reactionType == VideoReactionType.LIKE && video.getUser() != null) {
                notificationService.notifyNewLike(
                        video.getUser().getId(),
                        user.getId(),
                        user.getFullName(),
                        video.getId(),
                        video.getTitle(),
                        video.getThumbnailUrl() != null ? video.getThumbnailUrl() : video.getSplashImageUrl(),
                        user.getAvatar()
                );
            }
        }
        
        // Lấy số lượng sau khi thay đổi
        long likeCount = videoReactionRepository.countByVideoIdAndReactionType(videoId, VideoReactionType.LIKE);
        long dislikeCount = videoReactionRepository.countByVideoIdAndReactionType(videoId, VideoReactionType.DISLIKE);

        // Giữ đồng bộ cột favorites_count = số LIKE thật (dùng cho sort "Yêu thích")
        video.setFavoritesCount(likeCount);
        videoRepository.save(video);
        
        // Lấy reaction hiện tại của user (null nếu đã xóa)
        VideoReactionType userReaction = videoReactionRepository.findByUserIdAndVideoId(user.getId(), videoId)
                .stream().findFirst()
                .map(VideoReaction::getReactionType)
                .orElse(null);
        
        return ApiResponse.<VideoReactionResponse>builder()
                .code(1000)
                .message(isNewReaction ? "Đã thêm " + reactionType : (existingReaction.isPresent() && existingReaction.get().getReactionType() == reactionType ? "Đã bỏ " + reactionType : "Đã đổi " + reactionType))
                .result(VideoReactionResponse.builder()
                        .likeCount(likeCount)
                        .dislikeCount(dislikeCount)
                        .userReaction(userReaction)
                        .build())
                .build();
    }
    
    // Lấy số lượng reaction của video
    public VideoReactionResponse getReactionCounts(String videoId) {
        long likeCount = videoReactionRepository.countByVideoIdAndReactionType(videoId, VideoReactionType.LIKE);
        long dislikeCount = videoReactionRepository.countByVideoIdAndReactionType(videoId, VideoReactionType.DISLIKE);
        
        VideoReactionType userReaction = null;
        try {
            User user = getCurrentUser();
            userReaction = videoReactionRepository.findByUserIdAndVideoId(user.getId(), videoId)
                    .stream().findFirst()
                    .map(VideoReaction::getReactionType)
                    .orElse(null);
        } catch (Exception e) {
            // User chưa đăng nhập
        }
        
        return VideoReactionResponse.builder()
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .userReaction(userReaction)
                .build();
    }
    
    // Lấy danh sách video user đã thích (reaction LIKE), phân trang
    @Transactional(readOnly = true)
    public Page<VideoResponse> getLikedVideos(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return videoReactionRepository
                .findByUserIdAndReactionTypeOrderByCreatedAtDesc(user.getId(), VideoReactionType.LIKE, pageable)
                .map(r -> {
                    Video video = r.getVideo();
                    // Khởi tạo các quan hệ lazy trước khi map (tránh lỗi lazy init khi serialize)
                    Hibernate.initialize(video.getUser());
                    Hibernate.initialize(video.getCategories());
                    Hibernate.initialize(video.getTags());
                    return videoMapper.toVideoResponse(video);
                });
    }

    /**
     * Video đã thích bởi user chỉ định (tab "Video đã thích" trên trang kênh).
     * Hiện tất cả cho mọi người xem (kể cả video riêng tư), chỉ lọc video READY.
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getPublicLikedVideos(String userId, Pageable pageable) {
        return videoReactionRepository
                .findLikedVideosByUserId(userId, pageable)
                .map(r -> {
                    Video video = r.getVideo();
                    Hibernate.initialize(video.getCategories());
                    Hibernate.initialize(video.getTags());
                    return videoMapper.toVideoResponse(video);
                });
    }
    
    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}