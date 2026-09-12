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
}
