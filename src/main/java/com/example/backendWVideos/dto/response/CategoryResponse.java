package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    
    private String id;
    private String name;
    private String slug;
    private String description;
    private String color;
    private String icon;
    private Boolean isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByUsername; // Tên admin tạo (deprecated)
    private String createdByName; // Tên đầy đủ người tạo
}