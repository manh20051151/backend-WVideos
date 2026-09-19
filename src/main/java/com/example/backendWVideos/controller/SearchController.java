package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.ChannelSearchResult;
import com.example.backendWVideos.dto.response.NewsResponse;
import com.example.backendWVideos.dto.response.SearchSuggestResponse;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API tìm kiếm thông minh công khai: gợi ý nhanh trên header + tìm kiếm đầy đủ theo từng loại.
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "Search suggest", description = "Gợi ý nhanh khi gõ trên header: top video + kênh + tin tức khớp từ khóa")
    @GetMapping("/suggest")
    public ApiResponse<SearchSuggestResponse> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int videoLimit,
            @RequestParam(defaultValue = "3") int channelLimit,
            @RequestParam(defaultValue = "3") int newsLimit
    ) {
        SearchSuggestResponse result = searchService.suggest(q, clamp(videoLimit), clamp(channelLimit), clamp(newsLimit));
        return ApiResponse.<SearchSuggestResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Search videos", description = "Tìm video theo tiêu đề, mô tả hoặc tag (phân trang)")
    @GetMapping("/videos")
    public ApiResponse<Page<VideoResponse>> searchVideos(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ApiResponse.<Page<VideoResponse>>builder()
                .result(searchService.searchVideos(q, pageable))
                .build();
    }

    @Operation(summary = "Search channels", description = "Tìm kênh theo tên hiển thị hoặc slug kênh (phân trang)")
    @GetMapping("/channels")
    public ApiResponse<Page<ChannelSearchResult>> searchChannels(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ApiResponse.<Page<ChannelSearchResult>>builder()
                .result(searchService.searchChannels(q, pageable))
                .build();
    }

    @Operation(summary = "Search news", description = "Tìm tin tức đã xuất bản theo tiêu đề hoặc tóm tắt (phân trang)")
    @GetMapping("/news")
    public ApiResponse<Page<NewsResponse>> searchNews(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ApiResponse.<Page<NewsResponse>>builder()
                .result(searchService.searchNews(q, pageable))
                .build();
    }

    private int clamp(int limit) {
        return Math.min(Math.max(limit, 0), 10);
    }
}
