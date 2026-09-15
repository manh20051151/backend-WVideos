package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.AdminNotificationRequest;
import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.NotificationResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification", description = "Quản lý thông báo realtime")
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // JWT subject là email, nhưng notification lưu recipient theo user id (UUID),
    // nên cần resolve email -> user id trước khi truy vấn.
    private String getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return user.getId();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Operation(summary = "Lấy danh sách thông báo", description = "Phân trang, mới nhất trước")
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> result = notificationService.getNotifications(userId, pageable);
        return ApiResponse.<Page<NotificationResponse>>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Số thông báo chưa đọc")
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        String userId = getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ApiResponse.<Long>builder()
                .result(count)
                .build();
    }

    @Operation(summary = "Đánh dấu đã đọc một thông báo")
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable String id) {
        String userId = getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ApiResponse.<Void>builder()
                .message("Đã đánh dấu đã đọc")
                .build();
    }

    @Operation(summary = "Đánh dấu tất cả đã đọc")
    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        String userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.<Void>builder()
                .message("Đã đánh dấu tất cả là đã đọc")
                .build();
    }

    @Operation(summary = "Ẩn/Xóa một thông báo")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable String id) {
        String userId = getCurrentUserId();
        notificationService.delete(id, userId);
        return ApiResponse.<Void>builder()
                .message("Đã xóa thông báo")
                .build();
    }

    @Operation(summary = "Ẩn tất cả thông báo từ một kênh/người dùng")
    @DeleteMapping("/actor/{actorId}")
    public ApiResponse<Void> deleteAllFromActor(@PathVariable String actorId) {
        String userId = getCurrentUserId();
        notificationService.deleteAllFromActor(userId, actorId);
        return ApiResponse.<Void>builder()
                .message("Đã xóa tất cả thông báo từ nguồn này")
                .build();
    }

    @Operation(
            summary = "Admin gửi thông báo đến người dùng",
            description = "Truyền recipientId hoặc recipientEmail để gửi cho một người dùng cụ thể. " +
                    "Không truyền cả hai để gửi (broadcast) cho tất cả người dùng."
    )
    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> adminSendNotification(@Valid @RequestBody AdminNotificationRequest request) {
        User admin = getCurrentUser();
        long sentCount = notificationService.adminSend(
                admin.getId(),
                admin.getFullName(),
                admin.getAvatar(),
                request.getRecipientId(),
                request.getRecipientEmail(),
                request.getTitle(),
                request.getContent()
        );
        return ApiResponse.<Long>builder()
                .message("Đã gửi thông báo đến " + sentCount + " người dùng")
                .result(sentCount)
                .build();
    }
}
