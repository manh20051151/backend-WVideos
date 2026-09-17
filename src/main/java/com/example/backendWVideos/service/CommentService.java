package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.CommentRequest;
import com.example.backendWVideos.dto.request.CommentModerationRequest;
import com.example.backendWVideos.dto.response.CommentResponse;
import com.example.backendWVideos.dto.response.CommentReactionResponse;
import com.example.backendWVideos.entity.Comment;
import com.example.backendWVideos.entity.CommentReaction;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.CommentReactionType;
import com.example.backendWVideos.enums.CommentStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.CommentReactionRepository;
import com.example.backendWVideos.repository.CommentRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponse createComment(String userEmail, String videoId, CommentRequest request) {
        log.info("🚀 User {} đang tạo comment cho video {}", userEmail, videoId);
        
        // Validate user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Validate video
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        // Validate parent comment nếu đây là trả lời
        Comment directParent = null;
        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            directParent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
            if (directParent.getVideo() == null || !videoId.equals(directParent.getVideo().getId())
                    || Boolean.TRUE.equals(directParent.getIsDeleted())) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        // Gom reply về đúng nhóm: parent lưu ở cấp top-level (kiểu YouTube),
        // trả lời reply không lui sâu thêm nữa mà flat cùng cấp
        String content = request.getContent();
        Comment parent = null;
        if (directParent != null) {
            parent = directParent;
            while (parent.getParent() != null) {
                parent = parent.getParent();
            }
            if (directParent.getParent() != null) {
                // Trả lời một reply: thêm @tên người được trả lời vào nội dung
                String mentionName = directParent.getUser() != null
                        ? directParent.getUser().getFullName()
                        : "Người dùng";
                if (!content.startsWith("@")) {
                    content = "@" + mentionName + " " + content;
                }
            }
        }
        
        // Tạo comment và tự động duyệt (APPROVED)
        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .video(video)
                .parent(parent)
                .status(CommentStatus.APPROVED)  // Auto duyệt khi tạo
                .build();
        
        Comment saved = commentRepository.save(comment);
        
        // Tăng comment count (null-safe)
        long currentCount = video.getCommentsCount() != null ? video.getCommentsCount() : 0L;
        video.setCommentsCount(currentCount + 1);
        videoRepository.save(video);

        String thumbnail = video.getThumbnailUrl() != null ? video.getThumbnailUrl() : video.getSplashImageUrl();

        // Thông báo realtime
        if (directParent != null && directParent.getUser() != null
                && !directParent.getUser().getId().equals(user.getId())) {
            // Trả lời bình luận: thông báo cho chủ comment được trả lời
            String snippet = request.getContent();
            if (snippet != null && snippet.length() > 60) {
                snippet = snippet.substring(0, 60) + "...";
            }
            notificationService.notifyNewCommentReply(
                    directParent.getUser().getId(),
                    user.getId(),
                    user.getFullName(),
                    video.getId(),
                    video.getTitle(),
                    snippet,
                    thumbnail,
                    user.getAvatar()
            );
        } else if (directParent == null && video.getUser() != null) {
            // Bình luận mới: thông báo cho chủ video (không tự bình luận video của mình)
            notificationService.notifyNewComment(
                    video.getUser().getId(),
                    user.getId(),
                    user.getFullName(),
                    video.getId(),
                    video.getTitle(),
                    thumbnail,
                    user.getAvatar()
            );
        }
        
        log.info("✅ Comment created successfully: {} (status: APPROVED, parent: {})",
                saved.getId(), request.getParentId());
        
        return toCommentResponse(saved, userEmail);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getVideoComments(String videoId, Pageable pageable) {
        // Lấy user hiện tại (nếu có)
        String currentUserEmail = getCurrentUserEmail();

        // Danh sách top-level được xếp hạng theo điểm (like - dislike)
        log.info("🔍 Đang xem comments của video {} (xếp hạng theo like/dislike)", videoId);
        boolean admin = isAdmin();
        Page<Comment> pageData = commentRepository.findByVideoIdTopLevelRanked(videoId, pageable);

        Map<String, CommentReactionType> reactionMap = getReactionMapForPage(pageData.getContent());

        return pageData.map(c -> {
            if (!admin && c.getStatus() == CommentStatus.REJECTED) {
                return null;
            }
            return toCommentResponse(c, currentUserEmail, reactionMap);
        });
    }

    @Transactional(readOnly = true)
    public long getVideoCommentsCount(String videoId) {
        // Tổng comments gồm cả reply, loại bình luận đã xóa
        return commentRepository.countByVideoIdAndIsDeletedFalse(videoId);
    }

    /**
     * Lấy phản ứng của người dùng hiện tại cho một trang comment (batch, tránh N+1)
     */
    private Map<String, CommentReactionType> getReactionMapForPage(List<Comment> comments) {
        if (isAdmin()) {
            return Map.of(); // admin không cần highlight reaction của mình
        }
        String email = getCurrentUserEmail();
        if (email == null || email.isBlank()) {
            return Map.of();
        }
        return userRepository.findByEmail(email)
                .map(u -> commentReactionRepository.getReactionsFor(
                        u.getId(),
                        comments.stream().map(Comment::getId).collect(Collectors.toList())))
                .orElse(Map.of());
    }

    /**
     * Like / dislike comment. Gọi lại khi trùng loại thì bỏ phản ứng (toggle).
     */
    @Transactional
    public CommentReactionResponse toggleReaction(String userEmail, String commentId, CommentReactionType type) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }

        CommentReaction existing = commentReactionRepository
                .findByUserIdAndCommentId(user.getId(), commentId)
                .orElse(null);

        if (existing != null && existing.getReactionType() == type) {
            // Trùng loại: bỏ phản ứng
            commentReactionRepository.delete(existing);
        } else if (existing != null) {
            // Đổi LIKE <-> DISLIKE
            existing.setReactionType(type);
            commentReactionRepository.save(existing);
        } else {
            commentReactionRepository.save(CommentReaction.builder()
                    .user(user)
                    .comment(comment)
                    .reactionType(type)
                    .build());
        }

        // Đồng bộ lại counter trên comment (nguồn sự thật là bảng comment_reactions)
        long likeCount = commentReactionRepository.countByCommentIdAndReactionType(commentId, CommentReactionType.LIKE);
        long dislikeCount = commentReactionRepository.countByCommentIdAndReactionType(commentId, CommentReactionType.DISLIKE);
        comment.setLikeCount(likeCount);
        comment.setDislikeCount(dislikeCount);
        commentRepository.save(comment);

        log.info("✅ Reaction cập nhật: comment {}, like {}, dislike {}", commentId, likeCount, dislikeCount);

        return CommentReactionResponse.builder()
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .userReaction(type.name())
                .build();
    }

    @Transactional
    public CommentResponse moderateComment(String adminEmail, String commentId, CommentModerationRequest request) {
        log.info("🚀 Admin {} đang moderate comment {}", adminEmail, commentId);
        
        // Validate admin
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Validate comment
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        
        // Validate rejection reason nếu REJECTED
        if (request.getStatus() == CommentStatus.REJECTED && 
            (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        // Update content nếu admin edit
        if (request.getEditedContent() != null && !request.getEditedContent().trim().isEmpty()) {
            comment.setContent(request.getEditedContent());
            comment.setIsEdited(true);
            log.info("📝 Admin edited comment content");
        }
        
        // Update status
        comment.setStatus(request.getStatus());
        comment.setModeratedBy(admin);
        comment.setModeratedAt(LocalDateTime.now());
        
        if (request.getStatus() == CommentStatus.REJECTED) {
            comment.setRejectionReason(request.getRejectionReason());
        }
        
        Comment saved = commentRepository.save(comment);
        
        log.info("✅ Comment {} moderated: {}", commentId, request.getStatus());
        
        return toCommentResponse(saved, adminEmail);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getPendingComments(Pageable pageable) {
        log.info("🔍 Lấy danh sách pending comments");
        String currentUserEmail = getCurrentUserEmail();
        return commentRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            CommentStatus.PENDING, pageable
        ).map(c -> toCommentResponse(c, currentUserEmail));
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getAllComments(Pageable pageable) {
        log.info("🔍 Admin lấy tất cả comments");
        String currentUserEmail = getCurrentUserEmail();
        return commentRepository.findAll(pageable)
                .map(c -> toCommentResponse(c, currentUserEmail));
    }

    public long getPendingCommentsCount() {
        return commentRepository.countByStatusAndIsDeletedFalse(CommentStatus.PENDING);
    }

    @Transactional
    public CommentResponse editComment(String userEmail, String commentId, CommentRequest request) {
        log.info("🚀 User {} đang sửa comment {}", userEmail, commentId);
        
        // Validate user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Validate comment
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        
        // Check ownership
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Chỉ cho sửa khi đang PENDING hoặc APPROVED
        if (comment.getStatus() != CommentStatus.PENDING
                && comment.getStatus() != CommentStatus.APPROVED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        // Cập nhật nội dung và đánh dấu đã chỉnh sửa
        comment.setContent(request.getContent());
        comment.setIsEdited(true);
        Comment saved = commentRepository.save(comment);
        
        log.info("✅ Comment {} đã được sửa", commentId);
        
        return toCommentResponse(saved, userEmail);
    }

    @Transactional
    public void deleteComment(String userEmail, String videoId, String commentId) {
        log.info("🚀 User {} đang xóa comment {}", userEmail, commentId);
        
        // Validate user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Validate comment
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        
        // Check ownership
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Soft delete
        comment.setIsDeleted(true);
        commentRepository.save(comment);
        
        // Giảm comment count
        Video video = comment.getVideo();
        video.setCommentsCount(Math.max(0, video.getCommentsCount() - 1));
        videoRepository.save(video);
        
        log.info("✅ Comment {} deleted", commentId);
    }

    private CommentResponse toCommentResponse(Comment comment, String currentUserEmail) {
        return toCommentResponse(comment, currentUserEmail, Map.of());
    }

    private CommentResponse toCommentResponse(Comment comment, String currentUserEmail,
                                              Map<String, CommentReactionType> reactionMap) {
        // Xác định canView
        boolean canView = canViewCommentContent(comment, currentUserEmail);

        CommentReactionType myReaction = reactionMap.get(comment.getId());

        // Build response
        CommentResponse.CommentResponseBuilder builder = CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .userFullName(comment.getUser().getFullName())
                .userAvatar(comment.getUser().getAvatar())
                .videoId(comment.getVideo().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .isDeleted(comment.getIsDeleted())
                .isEdited(Boolean.TRUE.equals(comment.getIsEdited()))
                .status(comment.getStatus().name())
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0L)
                .dislikeCount(comment.getDislikeCount() != null ? comment.getDislikeCount() : 0L)
                .userReaction(myReaction != null ? myReaction.name() : null)
                .canView(canView);
        
        // Chỉ set content nếu canView = true
        if (canView) {
            builder.content(comment.getContent());
        }
        
        // Moderation info
        if (comment.getModeratedBy() != null) {
            builder.moderatedById(comment.getModeratedBy().getId())
                   .moderatedByName(comment.getModeratedBy().getFullName())
                   .moderatedAt(comment.getModeratedAt());
        }
        
        if (comment.getRejectionReason() != null) {
            builder.rejectionReason(comment.getRejectionReason());
        }
        
        // Replies (recursive)
        List<CommentResponse> replies = comment.getReplies().stream()
                .filter(r -> !r.getIsDeleted())
                .map(r -> toCommentResponse(r, currentUserEmail))
                .collect(Collectors.toList());
        builder.replies(replies);
        
        return builder.build();
    }

    private boolean canViewCommentContent(Comment comment, String currentUserEmail) {
        // Admin thấy nội dung tất cả
        if (isAdmin()) {
            return true;
        }
        
        // APPROVED: ai cũng thấy nội dung
        if (comment.getStatus() == CommentStatus.APPROVED) {
            return true;
        }
        
        // PENDING: không ai thấy nội dung (hiển thị "đang được kiểm duyệt")
        // REJECTED: không ai thấy nội dung
        return false;
    }

    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Không thể lấy current user: {}", e.getMessage());
        }
        return null;
    }

    private boolean isAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getAuthorities() != null) {
                return authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            }
        } catch (Exception e) {
            log.debug("Không thể check admin role: {}", e.getMessage());
        }
        return false;
    }
}
