package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.enums.NewsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponse {
    private String id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String thumbnailUrl;
    private NewsStatus status;
    private NewsCategoryResponse category;
    private String authorName;
    private int views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
