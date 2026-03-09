package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.CategoryCreateRequest;
import com.example.backendWVideos.dto.request.CategoryUpdateRequest;
import com.example.backendWVideos.dto.response.CategoryResponse;
import com.example.backendWVideos.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    /**
     * Lấy tất cả thể loại đang hoạt động (public)
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getActiveCategories() {
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Lấy tất cả thể loại (admin only) với phân trang và tìm kiếm
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CategoryResponse>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<CategoryResponse> categories = categoryService.getAllCategories(pageable, search);
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Lấy thể loại theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable String id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }
    
    /**
     * Test endpoint để debug
     */
    @PostMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("POST categories endpoint hoạt động!");
    }
    
    /**
     * Tạo thể loại mới (admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }
    
    /**
     * Cập nhật thể loại (admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(category);
    }
    
    /**
     * Xóa thể loại (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}