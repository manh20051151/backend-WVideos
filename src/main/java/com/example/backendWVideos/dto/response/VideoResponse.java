package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.enums.VideoStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private VideoStatus status;
    private Boolean isPublic;
    private List<CategoryResponse> categories; // Đổi từ CategoryResponse sang List<CategoryResponse>
    private String userId;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime uploadedToDoodStreamAt;
}
