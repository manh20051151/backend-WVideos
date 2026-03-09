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
            // Lấy thống kê 7 ngày gần nhất
            String sql = """
                SELECT 
                    DATE(created_at) as date,
                    COUNT(CASE WHEN table_name = 'users' THEN 1 END) as new_users,
                    COUNT(CASE WHEN table_name = 'videos' THEN 1 END) as new_videos,
                    COALESCE(SUM(CASE WHEN table_name = 'videos' THEN views ELSE 0 END), 0) as total_views
                FROM (
                    SELECT created_at, 'users' as table_name, 0 as views FROM users WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                    UNION ALL
                    SELECT created_at, 'videos' as table_name, views FROM videos WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND status != 'DELETED'
                ) combined
                GROUP BY DATE(created_at)
                ORDER BY date DESC
                LIMIT 7
                """;
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> row : results) {
                stats.add(DashboardStatsResponse.DailyStats.builder()
                        .date(row.get("date").toString())
                        .newUsers(((Number) row.get("new_users")).longValue())
                        .newVideos(((Number) row.get("new_videos")).longValue())
                        .totalViews(((Number) row.get("total_views")).longValue())
                        .build());
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê hàng ngày: {}", e.getMessage());
        }
        
        return stats;
    }
    
    private List<DashboardStatsResponse.CategoryStats> getTopCategories() {
        List<DashboardStatsResponse.CategoryStats> stats = new ArrayList<>();
        
        try {
            String sql = """
                SELECT 
                    c.id,
                    c.name,
                    c.icon,
                    c.color,
                    COUNT(v.id) as video_count,
                    COALESCE(SUM(v.views), 0) as total_views
                FROM categories c
                LEFT JOIN videos v ON c.id = v.category_id AND v.status != 'DELETED'
                WHERE c.is_active = true
                GROUP BY c.id, c.name, c.icon, c.color
                ORDER BY video_count DESC, total_views DESC
                LIMIT 10
                """;
            
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
            String sql = """
                SELECT 
                    u.id,
                    u.username,
                    u.email,
                    COUNT(v.id) as video_count,
                    COALESCE(SUM(v.views), 0) as total_views,
                    DATE(u.created_at) as join_date
                FROM users u
                LEFT JOIN videos v ON u.id = v.user_id AND v.status != 'DELETED'
                GROUP BY u.id, u.username, u.email, u.created_at
                ORDER BY video_count DESC, total_views DESC
                LIMIT 10
                """;
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> row : results) {
                stats.add(DashboardStatsResponse.UserStats.builder()
                        .userId((String) row.get("id"))
                        .username((String) row.get("username"))
                        .email((String) row.get("email"))
                        .videoCount(((Number) row.get("video_count")).longValue())
                        .totalViews(((Number) row.get("total_views")).longValue())
                        .joinDate(row.get("join_date").toString())
                        .build());
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy top users: {}", e.getMessage());
        }
        
        return stats;
    }
}