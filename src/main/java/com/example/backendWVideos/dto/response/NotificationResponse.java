package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    String id;
    NotificationType type;
    String title;
    String content;
    boolean read;
    String relatedId;     // videoId / channelId / commentId
    String videoSlug;     // Slug video cho link /watch/{slug} (chỉ với loại video)
    String actorId;       // người thực hiện hành động (nếu có)
    String actorName;
    String avatarUrl;     // ảnh đại diện người thực hiện
    String thumbnailUrl;  // thumbnail video liên quan
    LocalDateTime createdAt;
}
