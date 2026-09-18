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
    private String slug;            // Slug URL-friendly cho link /watch/{slug}
    private String streamUrl;       // direct mp4 URL đã resolve (từ cache Redis)
    private String thumbnailUrl;
    private String splashImageUrl;
    private String userFullName;
    private String userId;          // ID tác giả (dùng để theo dõi kênh)
    private String avatarUrl;       // ảnh đại diện người đăng
    private Long duration;
    private Long views;
    private Long likeCount;
    private Boolean isLiked;       // reaction của user hiện tại (nếu có)
    private Long price;            // giá video (VND), null/0 = miễn phí
    private Boolean isPaid;        // price != null && price > 0
    private Boolean purchased;     // user hiện tại đã mua chưa (chỉ khi đã đăng nhập)
    private Boolean isOwner;       // video thuộc sở hữu của user hiện tại
    private java.time.LocalDateTime createdAt; // dùng làm keyset cursor
}
