package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.FavoriteResponse;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.service.VideoFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Video Favorite", description = "Video Favorite APIs")
public class VideoFavoriteController {

    private final VideoFavoriteService videoFavoriteService;

    @Operation(summary = "Toggle favorite", description = "Thêm hoặc xóa yêu thích video")
    @PostMapping("/{videoId}/favorite")
    public ApiResponse<FavoriteResponse> toggleFavorite(@PathVariable String videoId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("User {} toggling favorite for video {}", userEmail, videoId);
        
        FavoriteResponse response = videoFavoriteService.toggleFavorite(userEmail, videoId);
        
        return ApiResponse.<FavoriteResponse>builder()
                .result(response)
                .message(response != null ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích")
                .build();
    }

    @Operation(summary = "Check if favorited", description = "Kiểm tra video đã được yêu thích chưa")
    @GetMapping("/{videoId}/favorite")
    public ApiResponse<Boolean> isFavorited(@PathVariable String videoId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isFavorited = videoFavoriteService.isFavorited(userEmail, videoId);
        
        return ApiResponse.<Boolean>builder()
                .result(isFavorited)
                .build();
    }

    @Operation(summary = "Get user favorites", description = "Lấy danh sách video đã yêu thích (phân trang)")
    @GetMapping("/favorites")
    public ApiResponse<Page<VideoResponse>> getUserFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponse> favorites = videoFavoriteService.getUserFavorites(userEmail, pageable);

        return ApiResponse.<Page<VideoResponse>>builder()
                .result(favorites)
                .build();
    }
}
