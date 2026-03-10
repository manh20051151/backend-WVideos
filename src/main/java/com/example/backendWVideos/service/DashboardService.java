package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.DashboardStatsResponse;
import com.example.backendWVideos.enums.VideoStatus;
import com.example.backendWVideos.repository.CategoryRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Lấy thống kê tổng quan cho dashboard admin
     */
    public DashboardStatsResponse getDashboardStats() {
        log.info("📊 Đang tính toán thống kê dashboard...");
        
        // Thống kê tổng quan
        Long totalUsers = userRepository.count();
        Long totalVideos = videoRepository.count();
        Long totalCategories = categoryRepository.count();
        Long totalViews = getTotalViews();
        
        // Thống kê video theo trạng thái
        Long videosUploading = videoRepository.countByStatus(VideoStatus.UPLOADING);
        Long videosProcessing = videoRepository.countByStatus(VideoStatus.PROCESSING);
        Long videosReady = videoRepository.countByStatus(VideoStatus.READY);
        Long videosFailed = videoRepository.countByStatus(VideoStatus.FAILED);
        
        // Thống kê 7 ngày gần nhất
        List<DashboardStatsResponse.DailyStats> dailyStats = getDailyStats();
        
        // Top categories
        List<DashboardStatsResponse.CategoryStats> topCategories = getTopCategories();
        
        // Top users
        List<DashboardStatsResponse.UserStats> topUsers = getTopUsers();
        
        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalVideos(totalVideos)
                .totalCategories(totalCategories)
                .totalViews(totalViews)
                .videosUploading(videosUploading)
                .videosProcessing(videosProcessing)
                .videosReady(videosReady)
                .videosFailed(videosFailed)
                .dailyStats(dailyStats)
                .topCategories(topCategories)
                .topUsers(topUsers)
                .build();
    }
    
    private Long getTotalViews() {
        try {
            String sql = "SELECT COALESCE(SUM(views), 0) FROM videos WHERE status != 'DELETED'";
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            log.error("Lỗi khi tính tổng lượt xem: {}", e.getMessage());
            return 0L;
        }
    }
    
    private List<DashboardStatsResponse.DailyStats> getDailyStats() {
        List<DashboardStatsResponse.DailyStats> stats = new ArrayList<>();
        
        try {
            // Tính ngày 7 ngày trước trong Java để tránh lỗi INTERVAL syntax
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            
            String userSql = "SELECT joined_date as created_at FROM users WHERE joined_date >= ?";
            String videoSql = "SELECT created_at, views FROM videos WHERE created_at >= ? AND status != 'DELETED'";
            
            // Lấy dữ liệu users
            List<Map<String, Object>> userResults = jdbcTemplate.queryForList(userSql, sevenDaysAgo);
            Map<String, Long> usersByDate = new java.util.HashMap<>();
            for (Map<String, Object> row : userResults) {
                String date = row.get("created_at").toString().substring(0, 10); // Lấy YYYY-MM-DD
                usersByDate.put(date, usersByDate.getOrDefault(date, 0L) + 1);
            }
            
            // Lấy dữ liệu videos
            List<Map<String, Object>> videoResults = jdbcTemplate.queryForList(videoSql, sevenDaysAgo);
            Map<String, Long> videosByDate = new java.util.HashMap<>();
            Map<String, Long> viewsByDate = new java.util.HashMap<>();
            for (Map<String, Object> row : videoResults) {
                String date = row.get("created_at").toString().substring(0, 10); // Lấy YYYY-MM-DD
                videosByDate.put(date, videosByDate.getOrDefault(date, 0L) + 1);
                Long views = ((Number) row.get("views")).longValue();
                viewsByDate.put(date, viewsByDate.getOrDefault(date, 0L) + views);
            }
            
            // Tạo danh sách 7 ngày gần nhất
            java.time.LocalDate today = java.time.LocalDate.now();
            for (int i = 0; i < 7; i++) {
                java.time.LocalDate date = today.minusDays(i);
                String dateStr = date.toString();
                
                stats.add(DashboardStatsResponse.DailyStats.builder()
                        .date(dateStr)
                        .newUsers(usersByDate.getOrDefault(dateStr, 0L))
                        .newVideos(videosByDate.getOrDefault(dateStr, 0L))
                        .totalViews(viewsByDate.getOrDefault(dateStr, 0L))
                        .build());
            }
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê hàng ngày: {}", e.getMessage());
            // Trả về dữ liệu mặc định nếu lỗi
            java.time.LocalDate today = java.time.LocalDate.now();
            for (int i = 0; i < 7; i++) {
                java.time.LocalDate date = today.minusDays(i);
                stats.add(DashboardStatsResponse.DailyStats.builder()
                        .date(date.toString())
                        .newUsers(0L)
                        .newVideos(0L)
                        .totalViews(0L)
                        .build());
            }
        }
        
        return stats;
    }
    
    private List<DashboardStatsResponse.CategoryStats> getTopCategories() {
        List<DashboardStatsResponse.CategoryStats> stats = new ArrayList<>();
        
        try {
            String sql = "SELECT " +
                    "c.id, " +
                    "c.name, " +
                    "c.icon, " +
                    "c.color, " +
                    "COUNT(DISTINCT vc.video_id) as video_count, " +
                    "COALESCE(SUM(v.views), 0) as total_views " +
                    "FROM categories c " +
                    "LEFT JOIN video_categories vc ON c.id = vc.category_id " +
                    "LEFT JOIN videos v ON vc.video_id = v.id AND v.status != 'DELETED' " +
                    "WHERE c.is_active = true " +
                    "GROUP BY c.id, c.name, c.icon, c.color " +
                    "ORDER BY video_count DESC, total_views DESC " +
                    "LIMIT 10";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> row : results) {
                stats.add(DashboardStatsResponse.CategoryStats.builder()
                        .categoryId((String) row.get("id"))
                        .categoryName((String) row.get("name"))
                        .categoryIcon((String) row.get("icon"))
                        .categoryColor((String) row.get("color"))
                        .videoCount(((Number) row.get("video_count")).longValue())
                        .totalViews(((Number) row.get("total_views")).longValue())
                        .build());
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy top categories: {}", e.getMessage());
        }
        
        return stats;
    }
    
    private List<DashboardStatsResponse.UserStats> getTopUsers() {
        List<DashboardStatsResponse.UserStats> stats = new ArrayList<>();
        
        try {
            // Query đơn giản nhất
            String sql = "SELECT id, username, email, joined_date FROM users ORDER BY joined_date DESC LIMIT 10";
            List<Map<String, Object>> userResults = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> row : userResults) {
                String userId = (String) row.get("id");
                
                // Đếm video của từng user riêng biệt
                String videoCountSql = "SELECT COUNT(*) as count FROM videos WHERE user_id = ? AND status != 'DELETED'";
                Long videoCount = jdbcTemplate.queryForObject(videoCountSql, Long.class, userId);
                
                // Tính tổng views của từng user riêng biệt
                String viewsSql = "SELECT COALESCE(SUM(views), 0) as total_views FROM videos WHERE user_id = ? AND status != 'DELETED'";
                Long totalViews = jdbcTemplate.queryForObject(viewsSql, Long.class, userId);
                
                stats.add(DashboardStatsResponse.UserStats.builder()
                        .userId(userId)
                        .username((String) row.get("username"))
                        .email((String) row.get("email"))
                        .videoCount(videoCount != null ? videoCount : 0L)
                        .totalViews(totalViews != null ? totalViews : 0L)
                        .joinDate(row.get("joined_date").toString().substring(0, 10))
                        .build());
            }
            
            // Sắp xếp theo video count và total views
            stats.sort((a, b) -> {
                int videoCompare = Long.compare(b.getVideoCount(), a.getVideoCount());
                if (videoCompare != 0) return videoCompare;
                return Long.compare(b.getTotalViews(), a.getTotalViews());
            });
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy top users: {}", e.getMessage());
        }
        
        return stats;
    }
}