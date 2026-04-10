package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.CommentRequest;
import com.example.backendWVideos.dto.request.CommentModerationRequest;
import com.example.backendWVideos.dto.response.CommentResponse;
import com.example.backendWVideos.entity.Comment;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.CommentStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse createComment(String userEmail, String videoId, CommentRequest request) {
        log.info("🚀 User {} đang tạo comment cho video {}", userEmail, videoId);
        
        // Validate user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Validate video
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        
        // Tạo comment với status PENDING
        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(user)
                .video(video)
                .status(CommentStatus.PENDING)  // Mặc định PENDING
                .build();
        
        Comment saved = commentRepository.save(comment);
        
        // Tăng comment count (null-safe)
        long currentCount = video.getCommentsCount() != null ? video.getCommentsCount() : 0L;
        video.setCommentsCount(currentCount + 1);
        videoRepository.save(video);
        
        log.info("✅ Comment created successfully: {} (status: PENDING)", saved.getId());
        
        return toCommentResponse(saved, userEmail);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getVideoComments(String videoId, Pageable pageable) {
        // Lấy user hiện tại (nếu có)
        String currentUserEmail = getCurrentUserEmail();
        
        if (isAdmin()) {
            // Admin: thấy tất cả (PENDING + APPROVED + REJECTED)
            log.info("🔍 Admin đang xem tất cả comments của video {}", videoId);
            return commentRepository.findByVideoIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
                videoId, pageable
            ).map(c -> toCommentResponse(c, currentUserEmail));
        }
        
        // User/Guest: thấy APPROVED + PENDING (nhưng PENDING sẽ ẩn nội dung)
        log.info("🔍 Đang xem comments của video {}", videoId);
        return commentRepository.findByVideoIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
            videoId, pageable
        ).map(c -> {
            // Lọc bỏ REJECTED comments cho user/guest
            if (c.getStatus() == CommentStatus.REJECTED) {
                return null;
            }
            return toCommentResponse(c, currentUserEmail);
        });
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
        
        // Chỉ cho sửa khi đang PENDING
        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        // Cập nhật nội dung
        comment.setContent(request.getContent());
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
        // Xác định canView
        boolean canView = canViewCommentContent(comment, currentUserEmail);
        
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
                .status(comment.getStatus().name())
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
