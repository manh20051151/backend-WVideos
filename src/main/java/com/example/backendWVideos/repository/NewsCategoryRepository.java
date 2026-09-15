package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsCategoryRepository extends JpaRepository<NewsCategory, String> {

    Optional<NewsCategory> findBySlug(String slug);

    Optional<NewsCategory> findByName(String name);

    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT c FROM NewsCategory c")
    Page<NewsCategory> findAllWithCreatedBy(Pageable pageable);

    @Query("SELECT c FROM NewsCategory c WHERE c.isActive = true ORDER BY c.sortOrder ASC, c.name ASC")
    List<NewsCategory> findAllActiveOrderBySortOrder();

    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT c FROM NewsCategory c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<NewsCategory> findBySearchQuery(@Param("search") String search, Pageable pageable);

    boolean existsByNameAndIdNot(String name, String id);

    boolean existsBySlugAndIdNot(String slug, String id);
}
