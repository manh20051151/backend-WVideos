package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.FooterLink;
import com.example.backendWVideos.enums.FooterSection;
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
public interface FooterLinkRepository extends JpaRepository<FooterLink, String> {

    // Lấy tất cả với phân trang - fetch createdBy
    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT f FROM FooterLink f")
    Page<FooterLink> findAllWithCreatedBy(Pageable pageable);

    // Lấy theo section với phân trang - fetch createdBy
    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT f FROM FooterLink f WHERE f.section = :section")
    Page<FooterLink> findBySection(@Param("section") FooterSection section, Pageable pageable);

    // Tìm kiếm footer link với phân trang - fetch createdBy
    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT f FROM FooterLink f WHERE " +
           "LOWER(f.label) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.href) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.createdByName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<FooterLink> findBySearchQuery(@Param("search") String search, Pageable pageable);

    // Lấy tất cả footer link đang hoạt động của một section, sắp xếp theo thứ tự
    List<FooterLink> findAllByIsActiveTrueAndSectionOrderBySortOrderAscLabelAsc(FooterSection section);

    // Kiểm tra label đã tồn tại trong section (trừ ID hiện tại)
    boolean existsByLabelAndSectionAndIdNot(String label, FooterSection section, String id);

    // Kiểm tra label đã tồn tại trong section
    boolean existsByLabelAndSection(String label, FooterSection section);
}