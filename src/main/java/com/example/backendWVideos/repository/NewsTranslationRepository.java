package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.NewsTranslation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsTranslationRepository extends JpaRepository<NewsTranslation, String> {

    Optional<NewsTranslation> findByNewsIdAndLocale(String newsId, String locale);

    List<NewsTranslation> findByNewsId(String newsId);

    List<NewsTranslation> findByNewsIdInAndLocale(List<String> newsIds, String locale);

    void deleteByNewsId(String newsId);

    long countByNewsId(String newsId);

    /**
     * Id các bài tin tức PUBLISHED còn thiếu bản dịch (dịch trước đó thất bại),
     * dùng cho scheduler backfill.
     */
    @Query("""
            SELECT n.id FROM News n
            WHERE n.status = 'PUBLISHED'
              AND (SELECT COUNT(t) FROM NewsTranslation t WHERE t.news.id = n.id) < :targetCount
            ORDER BY n.publishedAt DESC
            """)
    List<String> findNewsIdsWithIncompleteTranslations(@Param("targetCount") long targetCount, Pageable pageable);
}
