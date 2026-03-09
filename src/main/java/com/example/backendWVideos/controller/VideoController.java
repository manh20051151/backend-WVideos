package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.VideoUpdateRequest;
import com.example.backendWVideos.dto.request.VideoUploadRequest;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Video", description = "Video Management APIs")
public class VideoController {

    private final VideoService videoService;

    @Operation(summary = "Upload video", description = "Upload video lên DoodStream")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VideoResponse> uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") @Valid VideoUploadRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        log.info("📤 User {} đang upload video: {}", userEmail, request.getTitle());
        
        VideoResponse response = videoService.uploadVideo(userEmail, file, request);
        
        return ApiResponse.<VideoResponse>builder()
                .result(response)
                .build();
    }

    @Operation(summary = "Get my videos", description = "Lấy danh sách video của tôi")
    @GetMapping("/my-videos")
    public ApiResponse<Page<VideoResponse>> getMyVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<VideoResponse> videos = videoService.getMyVideos(userEmail, pageable);
        
        return ApiResponse.<Page<VideoResponse>>builder()
                .result(videos)
                .build();
    }

    @Operation(summary = "Get public videos", description = "Lấy danh sách video công khai")
    @GetMapping("/public")
    public ApiResponse<Page<VideoResponse>> getPublicVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<VideoResponse> videos = videoService.getPublicVideos(pageable);
        
        return ApiResponse.<Page<VideoResponse>>builder()
                .result(videos)
                .build();
    }

    @Operation(summary = "Get video by ID", description = "Lấy chi tiết video")
    @GetMapping("/{videoId}")
    public ApiResponse<VideoResponse> getVideoById(@PathVariable String videoId) {
        VideoResponse video = videoService.getVideoById(videoId);
        
        return ApiResponse.<VideoResponse>builder()
                .result(video)
                .build();
    }

    @Operation(summary = "Update video", description = "Cập nhật thông tin video")
    @PutMapping("/{videoId}")
    public ApiResponse<VideoResponse> updateVideo(
            @PathVariable String videoId,
            @RequestBody @Valid VideoUpdateRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        VideoResponse video = videoService.updateVideo(userEmail, videoId, request);
        
        return ApiResponse.<VideoResponse>builder()
                .result(video)
                .build();
    }

    @Operation(summary = "Delete video", description = "Xóa video")
    @DeleteMapping("/{videoId}")
    public ApiResponse<Void> deleteVideo(@PathVariable String videoId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        videoService.deleteVideo(userEmail, videoId);
        
        return ApiResponse.<Void>builder()
                .message("Xóa video thành công")
                .build();
    }

    @Operation(summary = "Increment views", description = "Tăng lượt xem video")
    @PostMapping("/{videoId}/view")
    public ApiResponse<Void> incrementViews(
            @PathVariable String videoId,
            HttpServletRequest request
    ) {
        String clientIp = getClientIpAddress(request);
        videoService.incrementViews(videoId, clientIp);
        
        return ApiResponse.<Void>builder()
                .message("Đã tăng lượt xem")
                .build();
    }
    
    /**
     * Lấy IP address thực của client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    @Operation(summary = "Sync video info from DoodStream", description = "Force sync thông tin video từ DoodStream")
    @PostMapping("/{videoId}/sync")
    public ApiResponse<VideoResponse> syncVideoInfo(
            @PathVariable String videoId
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("🔄 User {} đang sync thông tin video: {}", userEmail, videoId);
        
        VideoResponse response = videoService.syncVideoInfo(userEmail, videoId);
        return ApiResponse.<VideoResponse>builder()
                .result(response)
                .build();
    }
}
