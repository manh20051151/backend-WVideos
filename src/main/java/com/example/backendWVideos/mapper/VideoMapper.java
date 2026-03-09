package com.example.backendWVideos.mapper;

import com.example.backendWVideos.dto.response.CategoryResponse;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.entity.Video;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class VideoMapper {
    
    /**
     * Chuyển đổi Video entity sang VideoResponse DTO
     */
    public VideoResponse toVideoResponse(Video video) {
        if (video == null) {
            return null;
        }
        
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .fileCode(video.getFileCode())
                .downloadUrl(video.getDownloadUrl())
                .embedUrl(video.getEmbedUrl())
                .protectedEmbedUrl(video.getProtectedEmbedUrl())
                .protectedDownloadUrl(video.getProtectedDownloadUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .splashImageUrl(video.getSplashImageUrl())
                .fileSize(video.getFileSize())
                .duration(video.getDuration())
                .views(video.getViews())
                .status(video.getStatus())
                .isPublic(video.getIsPublic())
                .categories(mapCategoriesToResponse(video.getCategories()))
                .userId(video.getUser() != null ? video.getUser().getId() : null)
                .username(video.getUser() != null ? video.getUser().getUsername() : null)
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .uploadedToDoodStreamAt(video.getUploadedToDoodStreamAt())
                .build();
    }
    
    /**
     * Chuyển đổi Set<Category> sang List<CategoryResponse>
     */
    private List<CategoryResponse> mapCategoriesToResponse(java.util.Set<com.example.backendWVideos.entity.Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        
        return categories.stream()
                .map(this::mapCategoryToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Chuyển đổi Category entity sang CategoryResponse DTO
     */
    private CategoryResponse mapCategoryToResponse(com.example.backendWVideos.entity.Category category) {
        if (category == null) {
            return null;
        }
        
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .color(category.getColor())
                .icon(category.getIcon())
                .isActive(category.getIsActive())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdByUsername(category.getCreatedBy() != null ? category.getCreatedBy().getUsername() : null)
                .build();
    }
}