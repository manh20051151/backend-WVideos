package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.CommentCreateRequest;
import com.example.backendWVideos.dto.response.CommentResponse;
import com.example.backendWVideos.entity.Comment;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.CommentRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public CommentResponse createComment(String userEmail, String videoId, CommentCreateRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        
        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(user)
                .video(video)
                .build();
        
        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
            comment.setParent(parent);
        }
        
        Comment saved = commentRepository.save(comment);
        
        video.setCommentsCount(video.getCommentsCount() + 1);
        videoRepository.save(video);
        
        log.info("User {} commented on video {}", userEmail, videoId);
        
        return toCommentResponse(saved);
    }

    public Page<CommentResponse> getVideoComments(String videoId, Pageable pageable) {
        return commentRepository.findByVideoIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(videoId, pageable)
                .map(this::toCommentResponse);
    }

    @Transactional
    public void deleteComment(String userEmail, String commentId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        comment.setIsDeleted(true);
        commentRepository.save(comment);
        
        Video video = comment.getVideo();
        video.setCommentsCount(Math.max(0, video.getCommentsCount() - 1));
        videoRepository.save(video);
        
        log.info("User {} deleted comment {}", userEmail, commentId);
    }

    public List<CommentResponse> getCommentReplies(String parentId) {
        return commentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentId)
                .stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    private CommentResponse toCommentResponse(Comment comment) {
        List<CommentResponse> replies = comment.getReplies().stream()
                .filter(r -> !r.getIsDeleted())
                .map(r -> CommentResponse.builder()
                        .id(r.getId())
                        .content(r.getContent())
                        .userId(r.getUser().getId())
                        .userFullName(r.getUser().getFullName())
                        .videoId(r.getVideo().getId())
                        .parentId(r.getParent() != null ? r.getParent().getId() : null)
                        .createdAt(r.getCreatedAt())
                        .isDeleted(r.getIsDeleted())
                        .build())
                .collect(Collectors.toList());
        
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .userFullName(comment.getUser().getFullName())
                .videoId(comment.getVideo().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(replies)
                .createdAt(comment.getCreatedAt())
                .isDeleted(comment.getIsDeleted())
                .build();
    }
}
