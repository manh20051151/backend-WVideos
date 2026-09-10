package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.config.CdnProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortsResponse {
    private String id;
    private String title;
    private String streamUrl;       // direct mp4 URL đã resolve (từ cache Redis)
    private String thumbnailUrl;
    private String splashImageUrl;
    private String userFullName;
    private String avatarUrl;       // ảnh đại diện người đăng
    private Long duration;
    private Long views;
    private Long likeCount;
    private Boolean isLiked;       // reaction của user hiện tại (nếu có)
    private java.time.LocalDateTime createdAt; // dùng làm keyset cursor
}
