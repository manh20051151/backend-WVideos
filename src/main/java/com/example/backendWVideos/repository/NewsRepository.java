package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.News;
import com.example.backendWVideos.enums.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, String> {

    Optional<News> findBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "author"})
    @Query("SELECT n FROM News n")
    Page<News> findAllWithRelations(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "author"})
    @Query("SELECT n FROM News n WHERE n.status = :status")
    Page<News> findAllByStatusWithRelations(@Param("status") NewsStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "author"})
    @Query("SELECT n FROM News n WHERE n.status = :status AND n.category.id = :categoryId")
    Page<News> findAllByStatusAndCategoryWithRelations(@Param("status") NewsStatus status,
                                                       @Param("categoryId") String categoryId,
                                                       Pageable pageable);

    @EntityGraph(attributePaths = {"category", "author"})
    @Query("SELECT n FROM News n WHERE n.status = :status AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<News> searchPublished(@Param("status") NewsStatus status,
                               @Param("search") String search,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"category", "author"})
    @Query("SELECT n FROM News n WHERE " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<News> searchAll(@Param("search") String search, Pageable pageable);

    boolean existsBySlugAndIdNot(String slug, String id);
}
