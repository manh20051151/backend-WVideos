package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.VideoTranslation;
import com.example.backendWVideos.enums.VideoStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoTranslationRepository extends JpaRepository<VideoTranslation, String> {

    List<VideoTranslation> findByVideoIdOrderByLocaleAsc(String videoId);

    Optional<VideoTranslation> findByVideoIdAndLocale(String videoId, String locale);

    void deleteByVideoId(String videoId);

    long countByVideoId(String videoId);

    /**
     * Video còn thiếu bản dịch (dịch thất bại do lỗi mạng/quá tải và chưa được thử lại).
     */
    @Query("""
            SELECT v.id FROM Video v
            WHERE v.status IN :statuses
              AND v.title IS NOT NULL
              AND (SELECT COUNT(t) FROM VideoTranslation t WHERE t.video = v) < :targetCount
            """)
    List<String> findVideoIdsWithIncompleteTranslations(
            @Param("statuses") Collection<VideoStatus> statuses,
            @Param("targetCount") long targetCount,
            Pageable pageable);
}
