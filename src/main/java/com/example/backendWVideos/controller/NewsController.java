package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.NewsRequest;
import com.example.backendWVideos.dto.request.NewsTranslationUpsertRequest;
import com.example.backendWVideos.dto.response.NewsResponse;
import com.example.backendWVideos.dto.response.NewsTranslationResponse;
import com.example.backendWVideos.service.NewsService;
import com.example.backendWVideos.service.NewsTranslationService;
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
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final NewsTranslationService newsTranslationService;

    // ============ PUBLIC ============

    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getPublishedNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String search,
            java.util.Locale locale) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return ResponseEntity.ok(newsService.getPublishedNews(pageable, categoryId, search, locale));
    }

    /**
     * Chi tiết tin đã xuất bản - tiêu đề/tóm tắt/nội dung bản địa hóa
     * theo header Accept-Language (fallback tiếng Việt).
     */
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getPublishedNewsDetail(@PathVariable String id, java.util.Locale locale) {
        return ResponseEntity.ok(newsService.getPublishedNewsDetail(id, locale));
    }

    // ============ ADMIN ============

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<NewsResponse>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(newsService.getAllNews(pageable, search));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable String id) {
        return ResponseEntity.ok(newsService.getNewsById(id));
    }

    /**
     * Lấy mọi bản dịch của 1 bài tin (admin only).
     * Trả đủ các ngôn ngữ đích, ngôn ngữ chưa dịch thì các trường = null.
     */
    @GetMapping("/{id}/translations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NewsTranslationResponse>> getTranslations(@PathVariable String id) {
        return ResponseEntity.ok(newsTranslationService.getTranslationsForAdmin(id));
    }

    /**
     * Admin cập nhật bản dịch bài tin sau khi Gemini dịch tự động
     * (sửa lại cho tự nhiên, dịch tay ngôn ngữ còn thiếu, title rỗng -> xóa bản dịch).
     */
    @PutMapping("/{id}/translations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NewsTranslationResponse>> updateTranslations(
            @PathVariable String id,
            @Valid @RequestBody NewsTranslationUpsertRequest request) {
        return ResponseEntity.ok(newsTranslationService.updateTranslations(id, request.getTranslations()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.createNews(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> updateNews(
            @PathVariable String id, @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.ok(newsService.updateNews(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNews(@PathVariable String id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
