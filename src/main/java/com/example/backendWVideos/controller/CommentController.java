package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.CommentRequest;
import com.example.backendWVideos.dto.request.CommentModerationRequest;
import com.example.backendWVideos.dto.request.CommentReactionRequest;
import com.example.backendWVideos.dto.response.CommentResponse;
import com.example.backendWVideos.dto.response.CommentReactionResponse;
import com.example.backendWVideos.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comment", description = "Comment Management APIs")
public class CommentController {

    private final CommentService commentService;

    // ==================== USER ENDPOINTS ====================

    @Operation(summary = "Create comment", description = "Tạo bình luận mới (status = PENDING)")
    @PostMapping("/videos/{videoId}/comments")
    public ApiResponse<CommentResponse> createComment(
            @PathVariable String videoId,
            @RequestBody @Valid CommentRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        CommentResponse response = commentService.createComment(userEmail, videoId, request);
        
        return ApiResponse.<CommentResponse>builder()
                .result(response)
                .message("Bình luận đã được gửi và đang chờ duyệt")
                .build();
    }

    @Operation(summary = "Get video comments", description = "Lấy danh sách bình luận của video (xếp hạng theo like/dislike)")
    @GetMapping("/videos/{videoId}/comments")
    public ApiResponse<Page<CommentResponse>> getVideoComments(
            @PathVariable String videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommentResponse> comments = commentService.getVideoComments(
            videoId, 
            PageRequest.of(page, size)
        );
        
        return ApiResponse.<Page<CommentResponse>>builder()
                .result(comments)
                .build();
    }

    @Operation(summary = "Get comments count", description = "Tổng số bình luận của video (bao gồm cả trả lời, loại bình luận đã xóa)")
    @GetMapping("/videos/{videoId}/comments/count")
    public ApiResponse<Long> getCommentsCount(@PathVariable String videoId) {
        long count = commentService.getVideoCommentsCount(videoId);
        return ApiResponse.<Long>builder()
                .result(count)
                .build();
    }

    @Operation(summary = "React to comment", description = "Like hoặc dislike bình luận. Gọi lặp lại cùng loại sẽ bỏ phản ứng. Kết ảnh hưởng đến thứ tự hiển thị.")
    @PostMapping("/comments/{commentId}/reactions")
    public ApiResponse<CommentReactionResponse> reactToComment(
            @PathVariable String commentId,
            @RequestBody @Valid CommentReactionRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        CommentReactionResponse response = commentService.toggleReaction(userEmail, commentId, request.getReactionType());

        return ApiResponse.<CommentReactionResponse>builder()
                .result(response)
                .message("Đã cập nhật phản ứng bình luận")
                .build();
    }

    @Operation(summary = "Delete comment", description = "Xóa bình luận của mình")
    @DeleteMapping("/videos/{videoId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable String videoId,
            @PathVariable String commentId
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        commentService.deleteComment(userEmail, videoId, commentId);
        
        return ApiResponse.<Void>builder()
                .message("Xóa bình luận thành công")
                .build();
    }

    @Operation(summary = "Edit comment", description = "Sửa bình luận khi đang chờ duyệt (PENDING)")
    @PutMapping("/videos/{videoId}/comments/{commentId}")
    public ApiResponse<CommentResponse> editComment(
            @PathVariable String videoId,
            @PathVariable String commentId,
            @RequestBody @Valid CommentRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        CommentResponse response = commentService.editComment(userEmail, commentId, request);
        
        return ApiResponse.<CommentResponse>builder()
                .result(response)
                .message("Đã cập nhật bình luận")
                .build();
    }

    // ==================== ADMIN ENDPOINTS ====================

    @Operation(summary = "Get pending comments", description = "Admin: Lấy danh sách comment chờ duyệt")
    @GetMapping("/admin/comments/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<CommentResponse>> getPendingComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommentResponse> comments = commentService.getPendingComments(
            PageRequest.of(page, size)
        );
        
        return ApiResponse.<Page<CommentResponse>>builder()
                .result(comments)
                .build();
    }

    @Operation(summary = "Moderate comment", description = "Admin: Duyệt hoặc từ chối comment")
    @PostMapping("/admin/comments/{commentId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CommentResponse> moderateComment(
            @PathVariable String commentId,
            @RequestBody @Valid CommentModerationRequest request
    ) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        CommentResponse response = commentService.moderateComment(adminEmail, commentId, request);
        
        String message = request.getStatus().name().equals("APPROVED") 
            ? "Đã duyệt bình luận" 
            : "Đã từ chối bình luận";
        
        return ApiResponse.<CommentResponse>builder()
                .result(response)
                .message(message)
                .build();
    }

    @Operation(summary = "Get all comments", description = "Admin: Lấy tất cả comments (tìm kiếm + lọc theo trạng thái)")
    @GetMapping("/admin/comments")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<CommentResponse>> getAllComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.example.backendWVideos.enums.CommentStatus status
    ) {
        Page<CommentResponse> comments = commentService.getAllComments(
            PageRequest.of(page, size),
            search,
            status
        );
        
        return ApiResponse.<Page<CommentResponse>>builder()
                .result(comments)
                .build();
    }

    @Operation(summary = "Delete comment", description = "Admin: Xóa bình luận (kèm toàn bộ reply và reaction)")
    @DeleteMapping("/admin/comments/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> adminDeleteComment(@PathVariable String commentId) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        commentService.adminDeleteComment(adminEmail, commentId);

        return ApiResponse.<Void>builder()
                .message("Đã xóa bình luận")
                .build();
    }

    @Operation(summary = "Get pending comments count", description = "Admin: Đếm số comment chờ duyệt")
    @GetMapping("/admin/comments/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getPendingCommentsCount() {
        long count = commentService.getPendingCommentsCount();
        
        return ApiResponse.<Long>builder()
                .result(count)
                .build();
    }
}
