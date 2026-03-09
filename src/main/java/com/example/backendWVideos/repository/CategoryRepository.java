package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    
    // Tìm theo slug
    Optional<Category> findBySlug(String slug);
    
    // Tìm theo tên
    Optional<Category> findByName(String name);
    
    // Lấy tất cả thể loại đang hoạt động, sắp xếp theo thứ tự
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findAllActiveOrderBySortOrder();
    
    // Lấy tất cả thể loại sắp xếp theo thứ tự
    @Query("SELECT c FROM Category c ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findAllOrderBySortOrder();
    
    // Tìm kiếm thể loại với phân trang
    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.createdByName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Category> findBySearchQuery(@Param("search") String search, Pageable pageable);
    
    // Kiểm tra tên đã tồn tại (trừ ID hiện tại)
    boolean existsByNameAndIdNot(String name, String id);
    
    // Kiểm tra slug đã tồn tại (trừ ID hiện tại)
    boolean existsBySlugAndIdNot(String slug, String id);
}