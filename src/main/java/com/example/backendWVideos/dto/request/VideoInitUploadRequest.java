package com.example.backendWVideos.dto.request;

import lombok.Data;

@Data
public class VideoInitUploadRequest {
    private String title;
    private String description;
    private Boolean isPublic;
    private java.util.List<String> categoryIds;
    private java.util.Set<String> tags;
    private String thumbnailUrl;

    // Thời lượng video (giây) - frontend đọc từ metadata trước khi upload
    private Long duration;
}
