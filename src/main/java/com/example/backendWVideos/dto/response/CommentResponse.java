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
    String videoTitle;  // Tiêu đề video (cho trang admin)
    String videoSlug;   // Slug video cho link /watch/{slug}
    String parentId;
    List<CommentResponse> replies;
    LocalDateTime createdAt;
    Boolean isDeleted;
    Boolean isEdited;  // Bình luận đã được chỉnh sửa
    
    // Moderation fields
    String status;  // PENDING, APPROVED, REJECTED
    String moderatedById;
    String moderatedByName;
    LocalDateTime moderatedAt;
    String rejectionReason;
    Boolean canView;  // Frontend dùng để quyết định hiện/ẩn content

    // Reactions
    Long likeCount;
    Long dislikeCount;
    String userReaction;  // LIKE, DISLIKE hoặc null (của người xem hiện tại)
}
