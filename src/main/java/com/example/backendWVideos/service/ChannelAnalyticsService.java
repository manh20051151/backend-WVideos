package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.ChannelAnalyticsResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.VideoStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.SubscriptionRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoFavoriteRepository;
import com.example.backendWVideos.repository.VideoReactionRepository;
import com.example.backendWVideos.repository.VideoRepository;
import com.example.backendWVideos.repository.VideoViewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tổng hợp số liệu phân tích cho kênh của chính người dùng đăng nhập:
 * người xem, tăng trưởng đăng ký, hiệu suất video và cơ cấu trạng thái.
 */
@Service
@RequiredArgsConstructor
public class ChannelAnalyticsService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final VideoViewLogRepository videoViewLogRepository;
    private final VideoReactionRepository videoReactionRepository;
    private final VideoFavoriteRepository videoFavoriteRepository;
    private final SubscriptionRepository subscriptionRepository;

    private static final int TREND_DAYS = 30;
    private static final int TREND_ALL_CAP_DAYS = 1095; // "toàn bộ thời gian" hiển thị tối đa 3 năm
    private static final int TOP_LIMIT = 8;

    /**
     * @param trend khoảng thời gian của biểu đồ xu hướng: số ngày (vd "7", "30", "90")
     *              hoặc "all" = từ hoạt động đầu tiên của kênh (có giới hạn trên)
     */
    @Transactional(readOnly = true)
    public ChannelAnalyticsResponse getMyChannelAnalytics(String trend) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        String userId = user.getId();

        LocalDate today = LocalDate.now();
        LocalDate trendStart = resolveTrendStart(trend, today, userId);
        LocalDateTime since = trendStart.atStartOfDay();

        // ----- Xu hướng theo ngày (theo khoảng đã chọn) -----
        Map<String, Long> viewsByDay = toDayCountMap(
                videoViewLogRepository.countOwnerViewsPerDay(userId, since));
        List<ChannelAnalyticsResponse.DailyPoint> viewTrend = fillTrend(viewsByDay, trendStart, today);
        long viewsToday = viewsByDay.getOrDefault(today.toString(), 0L);

        // KPI nạp/lượt xem dùng cửa sổ cố định, không đổi theo khoảng biểu đồ
        long views7d = videoViewLogRepository.countOwnerViewsSince(
                userId, today.minusDays(6).atStartOfDay());
        long views30d = videoViewLogRepository.countOwnerViewsSince(
                userId, today.minusDays(TREND_DAYS - 1L).atStartOfDay());

        // ----- Tăng trưởng người đăng ký -----
        Map<String, Long> subsByDay = toDayCountMap(
                subscriptionRepository.countDailyNewSubscribers(userId, since));
        List<ChannelAnalyticsResponse.DailyPoint> subscriberTrend = fillTrend(subsByDay, trendStart, today);
        long newSubscribers30d = subscriptionRepository.countByChannelIdAndSubscribedAtAfter(
                userId, today.minusDays(TREND_DAYS - 1L).atStartOfDay());
        long subscriberCount = subscriptionRepository.countByChannelId(userId);

        // ----- Tổng hợp cộng dồn -----
        long videoCount = videoRepository.countByUserIdAndStatusNot(userId, VideoStatus.DELETED);
        long totalViews = nvl(videoRepository.getTotalViewsByUserId(userId, VideoStatus.DELETED));
        List<Object[]> engagement = videoRepository.sumOwnerEngagement(userId);
        long totalComments = engagement.isEmpty() ? 0 : ((Number) engagement.get(0)[0]).longValue();
        long totalLikes = videoReactionRepository.countOwnerLikes(userId);
        long totalFavorites = videoFavoriteRepository.countOwnerFavorites(userId);

        // ----- Cơ cấu trạng thái video -----
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (VideoStatus s : new VideoStatus[]{VideoStatus.READY, VideoStatus.PROCESSING,
                VideoStatus.UPLOADING, VideoStatus.FAILED}) {
            statusBreakdown.put(s.name(), 0L);
        }
        for (Object[] row : videoRepository.countOwnerVideosByStatus(userId)) {
            statusBreakdown.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        // ----- Top video theo lượt xem cộng dồn -----
        Map<String, Long> likesByVideo = new HashMap<>();
        for (Object[] row : videoReactionRepository.countOwnerLikesByVideo(userId)) {
            likesByVideo.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        List<ChannelAnalyticsResponse.TopVideo> topVideos = new ArrayList<>();
        for (Video v : videoRepository.findByUserIdAndStatusNot(
                userId, VideoStatus.DELETED,
                PageRequest.of(0, TOP_LIMIT, Sort.by(Sort.Direction.DESC, "views")))) {
            topVideos.add(ChannelAnalyticsResponse.TopVideo.builder()
                    .id(v.getId())
                    .title(v.getTitle())
                    .thumbnailUrl(v.getThumbnailUrl() != null ? v.getThumbnailUrl() : v.getSplashImageUrl())
                    .views(v.getViews() != null ? v.getViews() : 0L)
                    .likes(likesByVideo.getOrDefault(v.getId(), 0L))
                    .comments(v.getCommentsCount() != null ? v.getCommentsCount() : 0L)
                    .publishedAt(v.getCreatedAt() != null ? v.getCreatedAt().toLocalDate().toString() : null)
                    .build());
        }

        return ChannelAnalyticsResponse.builder()
                .videoCount(videoCount)
                .totalViews(totalViews)
                .totalLikes(totalLikes)
                .totalComments(totalComments)
                .totalFavorites(totalFavorites)
                .subscriberCount(subscriberCount)
                .newSubscribers30d(newSubscribers30d)
                .viewsToday(viewsToday)
                .views7d(views7d)
                .views30d(views30d)
                .viewTrend(viewTrend)
                .subscriberTrend(subscriberTrend)
                .statusBreakdown(statusBreakdown)
                .topVideos(topVideos)
                .build();
    }

    // Xác định ngày bắt đầu chuỗi xu hướng theo tham số trend
    private LocalDate resolveTrendStart(String trend, LocalDate today, String userId) {
        if (trend != null && trend.matches("\\d+")) {
            int days = Math.min(Math.max(Integer.parseInt(trend), 1), TREND_ALL_CAP_DAYS);
            return today.minusDays(days - 1L);
        }
        if ("all".equalsIgnoreCase(trend)) {
            LocalDateTime earliest = null;
            LocalDateTime videoAt = videoRepository.oldestOwnerVideoCreatedAt(userId);
            LocalDateTime viewAt = videoViewLogRepository.oldestOwnerViewAt(userId);
            LocalDateTime subAt = subscriptionRepository.oldestChannelSubscriberAt(userId);
            for (LocalDateTime candidate : new LocalDateTime[]{videoAt, viewAt, subAt}) {
                if (candidate != null && (earliest == null || candidate.isBefore(earliest))) {
                    earliest = candidate;
                }
            }
            if (earliest == null) {
                return today.minusDays(TREND_DAYS - 1L);
            }
            LocalDate floor = today.minusDays(TREND_ALL_CAP_DAYS - 1L);
            LocalDate start = earliest.toLocalDate();
            return start.isBefore(floor) ? floor : start;
        }
        return today.minusDays(TREND_DAYS - 1L);
    }

    private Map<String, Long> toDayCountMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(toDateString(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }

    private String toDateString(Object value) {
        if (value instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (value instanceof LocalDate ld) return ld.toString();
        if (value instanceof LocalDateTime ldt) return ldt.toLocalDate().toString();
        return String.valueOf(value);
    }

    private List<ChannelAnalyticsResponse.DailyPoint> fillTrend(Map<String, Long> byDay,
                                                                LocalDate start, LocalDate end) {
        List<ChannelAnalyticsResponse.DailyPoint> trend = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            trend.add(ChannelAnalyticsResponse.DailyPoint.builder()
                    .date(d.toString())
                    .count(byDay.getOrDefault(d.toString(), 0L))
                    .build());
        }
        return trend;
    }

    private long nvl(Long value) {
        return value != null ? value : 0L;
    }
}
