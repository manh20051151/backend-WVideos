package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.CommentCreateRequest;
import com.example.backendWVideos.dto.response.CommentResponse;
import com.example.backendWVideos.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comment", description = "Comment APIs")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Create comment", description = "Tạo bình luận mới")
    @PostMapping("/{videoId}/comments")
    public ApiResponse<CommentResponse> createComment(
            @PathVariable String videoId,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("User {} creating comment on video {}", userEmail, videoId);
        
        CommentResponse response = commentService.createComment(userEmail, videoId, request);
        
        return ApiResponse.<CommentResponse>builder()
                .result(response)
                .message("Bình luận thành công")
                .build();
    }

    @Operation(summary = "Get video comments", description = "Lấy danh sách bình luận của video")
    @GetMapping("/{videoId}/comments")
    public ApiResponse<Page<CommentResponse>> getVideoComments(
            @PathVariable String videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommentResponse> comments = commentService.getVideoComments(videoId, PageRequest.of(page, size));
        
        return ApiResponse.<Page<CommentResponse>>builder()
                .result(comments)
                .build();
    }

    @Operation(summary = "Delete comment", description = "Xóa bình luận")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable String commentId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("User {} deleting comment {}", userEmail, commentId);
        
        commentService.deleteComment(userEmail, commentId);
        
        return ApiResponse.<Void>builder()
                .message("Xóa bình luận thành công")
                .build();
    }

    @Operation(summary = "Get comment replies", description = "Lấy danh sách reply của comment")
    @GetMapping("/comments/{commentId}/replies")
    public ApiResponse<List<CommentResponse>> getCommentReplies(@PathVariable String commentId) {
        List<CommentResponse> replies = commentService.getCommentReplies(commentId);
        
        return ApiResponse.<List<CommentResponse>>builder()
                .result(replies)
                .build();
    }
}
