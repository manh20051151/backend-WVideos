package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.VideoReactionResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.entity.VideoReaction;
import com.example.backendWVideos.enums.VideoReactionType;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoReactionRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoReactionService {
    
    private final VideoReactionRepository videoReactionRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    
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
        
        // Kiểm tra reaction hiện tại
        Optional<VideoReaction> existingReaction = videoReactionRepository.findByUserIdAndVideoId(user.getId(), videoId);
        
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
        }
        
        // Lấy số lượng sau khi thay đổi
        long likeCount = videoReactionRepository.countByVideoIdAndReactionType(videoId, VideoReactionType.LIKE);
        long dislikeCount = videoReactionRepository.countByVideoIdAndReactionType(videoId, VideoReactionType.DISLIKE);
        
        // Lấy reaction hiện tại của user (null nếu đã xóa)
        VideoReactionType userReaction = videoReactionRepository.findByUserIdAndVideoId(user.getId(), videoId)
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
    
    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}