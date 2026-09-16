package com.example.backendWVideos.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Thống kê tổng hợp cho kênh của chính người dùng (tab Phân tích trong profile).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelAnalyticsResponse {

    private long videoCount;          // Số video (không tính đã xóa)
    private long totalViews;          // Tổng lượt xem cộng dồn
    private long totalLikes;          // Tổng lượt thích trên các video của kênh
    private long totalComments;       // Tổng bình luận cộng dồn
    private long totalFavorites;      // Tổng lượt lưu cộng dồn
    private long subscriberCount;     // Người đăng ký hiện tại
    private long newSubscribers30d;   // Người đăng ký mới 30 ngày
    private long viewsToday;          // Lượt xem hôm nay
    private long views7d;             // Lượt xem 7 ngày
    private long views30d;            // Lượt xem 30 ngày

    private List<DailyPoint> viewTrend;        // Lượt xem theo ngày, 30 ngày gần nhất (đủ 30 điểm)
    private List<DailyPoint> subscriberTrend;  // Người đăng ký mới theo ngày, 30 ngày gần nhất
    private Map<String, Long> statusBreakdown; // READY / PROCESSING / UPLOADING / FAILED
    private List<TopVideo> topVideos;          // Top video theo lượt xem cộng dồn

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPoint {
        private String date;   // yyyy-MM-dd
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopVideo {
        private String id;
        private String title;
        private String thumbnailUrl;
        private long views;
        private long likes;
        private long comments;
        private String publishedAt; // yyyy-MM-dd
    }
}
