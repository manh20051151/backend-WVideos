package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminNotificationRequest {
    // Gửi cho một người dùng cụ thể (bắt buộc một trong hai nếu không broadcast)
    String recipientId;
    String recipientEmail;

    @NotBlank(message = "Tiêu đề không được để trống")
    String title;

    @NotBlank(message = "Nội dung không được để trống")
    String content;
}
