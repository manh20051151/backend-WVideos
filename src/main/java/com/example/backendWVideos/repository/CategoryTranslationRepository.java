package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.CategoryTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, String> {

    List<CategoryTranslation> findByOwnerTypeAndOwnerId(String ownerType, String ownerId);

    List<CategoryTranslation> findByOwnerTypeAndOwnerIdInAndLocale(String ownerType, List<String> ownerIds, String locale);

    Optional<CategoryTranslation> findFirstByOwnerTypeAndOwnerIdAndLocale(String ownerType, String ownerId, String locale);

    void deleteByOwnerTypeAndOwnerId(String ownerType, String ownerId);

    long countByOwnerTypeAndOwnerId(String ownerType, String ownerId);

    /**
     * Số bản dịch hiện có của từng danh mục, gom theo ownerId.
     * Dùng để scheduler tìm danh mục còn thiếu bản dịch.
     */
    @Query("""
            SELECT t.ownerId, COUNT(t) FROM CategoryTranslation t
            WHERE t.ownerType = :ownerType
            GROUP BY t.ownerId
            """)
    List<Object[]> countByOwnerGrouped(@Param("ownerType") String ownerType);
}
