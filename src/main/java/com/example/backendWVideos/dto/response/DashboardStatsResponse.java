package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    
    // Thống kê tổng quan
    private Long totalUsers;
    private Long totalVideos;
    private Long totalCategories;
    private Long totalViews;
    
    // Thống kê video theo trạng thái
    private Long videosUploading;
    private Long videosProcessing;
    private Long videosReady;
    private Long videosFailed;
    
    // Thống kê theo thời gian (7 ngày gần nhất)
    private List<DailyStats> dailyStats;
    
    // Top categories
    private List<CategoryStats> topCategories;
    
    // Top users
    private List<UserStats> topUsers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private String date; // YYYY-MM-DD
        private Long newUsers;
        private Long newVideos;
        private Long totalViews;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private String categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private Long videoCount;
        private Long totalViews;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private String userId;
        private String username;
        private String email;
        private Long videoCount;
        private Long totalViews;
        private String joinDate;
    }
}