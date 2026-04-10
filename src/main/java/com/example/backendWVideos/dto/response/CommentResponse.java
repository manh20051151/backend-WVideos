package com.example.backendWVideos.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentResponse {
    String id;
    String content;
    String userId;
    String userFullName;
    String userAvatar;
    String videoId;
    String parentId;
    List<CommentResponse> replies;
    LocalDateTime createdAt;
    Boolean isDeleted;
    
    // Moderation fields
    String status;  // PENDING, APPROVED, REJECTED
    String moderatedById;
    String moderatedByName;
    LocalDateTime moderatedAt;
    String rejectionReason;
    Boolean canView;  // Frontend dùng để quyết định hiện/ẩn content
}
