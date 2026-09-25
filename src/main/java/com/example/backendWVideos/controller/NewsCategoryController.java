package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.CategoryTranslationUpsertRequest;
import com.example.backendWVideos.dto.request.NewsCategoryRequest;
import com.example.backendWVideos.dto.response.CategoryTranslationResponse;
import com.example.backendWVideos.dto.response.NewsCategoryResponse;
import com.example.backendWVideos.service.NewsCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news-categories")
@RequiredArgsConstructor
public class NewsCategoryController {

    private final NewsCategoryService newsCategoryService;

    @GetMapping
    public ResponseEntity<List<NewsCategoryResponse>> getActiveCategories(java.util.Locale locale) {
        return ResponseEntity.ok(newsCategoryService.getActiveCategories(locale));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<NewsCategoryResponse>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(newsCategoryService.getAllCategories(pageable, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsCategoryResponse> getCategoryById(@PathVariable String id) {
        return ResponseEntity.ok(newsCategoryService.getCategoryById(id));
    }

    /**
     * Lấy mọi bản dịch tên của 1 danh mục tin tức (admin only).
     * Trả đủ các ngôn ngữ đích, ngôn ngữ chưa dịch thì name = null.
     */
    @GetMapping("/{id}/translations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryTranslationResponse>> getTranslations(@PathVariable String id) {
        return ResponseEntity.ok(newsCategoryService.getTranslations(id));
    }

    /**
     * Admin cập nhật bản dịch tên danh mục tin tức sau khi Gemini dịch tự động
     * (sửa lại cho tự nhiên, dịch tay ngôn ngữ còn thiếu, name rỗng -> xóa bản dịch).
     */
    @PutMapping("/{id}/translations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryTranslationResponse>> updateTranslations(
            @PathVariable String id,
            @Valid @RequestBody CategoryTranslationUpsertRequest request) {
        return ResponseEntity.ok(newsCategoryService.updateTranslations(id, request.getTranslations()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsCategoryResponse> createCategory(@Valid @RequestBody NewsCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsCategoryResponse> updateCategory(
            @PathVariable String id, @Valid @RequestBody NewsCategoryRequest request) {
        return ResponseEntity.ok(newsCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        newsCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
