package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.NewsCategoryRequest;
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
    public ResponseEntity<List<NewsCategoryResponse>> getActiveCategories() {
        return ResponseEntity.ok(newsCategoryService.getActiveCategories());
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
