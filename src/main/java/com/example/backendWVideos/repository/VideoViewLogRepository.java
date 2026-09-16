package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.VideoViewLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoViewLogRepository extends JpaRepository<VideoViewLog, Long> {

    /**
     * Top video theo số lượt xem trong khoảng thời gian (since),
     * chỉ tính video công khai và đã sẵn sàng (READY).
     */
    @Query(value = """
            SELECT vl.video_id
            FROM video_view_logs vl
            WHERE vl.viewed_at >= :since
              AND vl.video_id IN (SELECT id FROM videos WHERE status = 'READY')
            GROUP BY vl.video_id
            ORDER BY COUNT(vl.id) DESC
            """, nativeQuery = true)
    List<String> findTrendingVideoIds(@Param("since") LocalDateTime since, Pageable pageable);

    @Query(value = """
            SELECT COUNT(DISTINCT vl.video_id)
            FROM video_view_logs vl
            WHERE vl.viewed_at >= :since
              AND vl.video_id IN (SELECT id FROM videos WHERE status = 'READY')
            """, nativeQuery = true)
    long countTrendingVideoIds(@Param("since") LocalDateTime since);

    @Modifying
    @Query(value = "DELETE FROM video_view_logs WHERE viewed_at < :before", nativeQuery = true)
    int deleteBefore(@Param("before") LocalDateTime before);

    // === Thống kê kênh: lượt xem theo chủ video ===

    // Số lượt xem theo từng ngày của các video thuộc về userId (từ mốc since)
    @Query(value = """
            SELECT DATE(vl.viewed_at) AS d, COUNT(*) AS c
            FROM video_view_logs vl
            JOIN videos v ON v.id = vl.video_id
            WHERE v.user_id = :userId AND vl.viewed_at >= :since
            GROUP BY DATE(vl.viewed_at)
            ORDER BY d
            """, nativeQuery = true)
    List<Object[]> countOwnerViewsPerDay(@Param("userId") String userId, @Param("since") LocalDateTime since);

    // Tổng lượt xem của kênh trong khoảng thời gian (từ mốc since)
    @Query(value = """
            SELECT COUNT(*)
            FROM video_view_logs vl
            JOIN videos v ON v.id = vl.video_id
            WHERE v.user_id = :userId AND vl.viewed_at >= :since
            """, nativeQuery = true)
    long countOwnerViewsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    // Bản ghi lượt xem sớm nhất trên các video của kênh - dùng cho xu hướng "toàn bộ thời gian"
    @Query(value = """
            SELECT MIN(vl.viewed_at)
            FROM video_view_logs vl
            JOIN videos v ON v.id = vl.video_id
            WHERE v.user_id = :userId
            """, nativeQuery = true)
    LocalDateTime oldestOwnerViewAt(@Param("userId") String userId);
}
