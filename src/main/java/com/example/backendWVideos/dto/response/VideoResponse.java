package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.enums.VideoStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    
    private String id;
    private String title;
    private String description;
    private String fileCode;
    private String downloadUrl;
    private String embedUrl;
    private String protectedEmbedUrl;
    private String protectedDownloadUrl;
    private String thumbnailUrl;
    private String splashImageUrl;
    private Long fileSize;
    private Long duration;
    private Long views;
    private Long favoritesCount;
    private Long commentsCount;
    private VideoStatus status;
    private Boolean isPublic;
    private List<CategoryResponse> categories;
    private Set<String> tags;
    private String userId;
    private String userFullName;
    private Long subscriberCount;
    private Boolean isSubscribed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime uploadedToDoodStreamAt;
}
