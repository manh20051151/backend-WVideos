package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.NavItem;
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
public interface NavItemRepository extends JpaRepository<NavItem, String> {
    
    // Tìm theo slug
    Optional<NavItem> findBySlug(String slug);
    
    // Tìm theo label
    Optional<NavItem> findByLabel(String label);
    
    // Lấy tất cả với phân trang - fetch createdBy
    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT n FROM NavItem n")
    Page<NavItem> findAllWithCreatedBy(Pageable pageable);
    
    // Lấy tất cả nav item đang hoạt động, sắp xếp theo thứ tự - fetch createdBy
    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT n FROM NavItem n WHERE n.isActive = true ORDER BY n.sortOrder ASC, n.label ASC")
    List<NavItem> findAllActiveOrderBySortOrder();
    
    // Tìm kiếm nav item với phân trang - fetch createdBy
    @EntityGraph(attributePaths = {"createdBy"})
    @Query("SELECT n FROM NavItem n WHERE " +
           "LOWER(n.label) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.slug) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.href) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.createdByName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<NavItem> findBySearchQuery(@Param("search") String search, Pageable pageable);
    
    // Kiểm tra label đã tồn tại (trừ ID hiện tại)
    boolean existsByLabelAndIdNot(String label, String id);
    
    // Kiểm tra slug đã tồn tại (trừ ID hiện tại)
    boolean existsBySlugAndIdNot(String slug, String id);
}
