package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.VideoUpdateRequest;
import com.example.backendWVideos.dto.request.VideoUploadRequest;
import com.example.backendWVideos.dto.request.VideoInitUploadRequest;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.dto.response.VideoInitUploadResponse;
import com.example.backendWVideos.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Video", description = "Video Management APIs")
public class VideoController {

    private final VideoService videoService;
    private final com.example.backendWVideos.service.StreamtapeService streamtapeService;
    
    private RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Operation(summary = "Init upload", description = "Khởi tạo upload - tạo video record và lấy upload server")
    @PostMapping("/init-upload")
    public ApiResponse<VideoInitUploadResponse> initUpload(
            @RequestBody @Valid VideoInitUploadRequest request
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        log.info("🚀 User {} đang init upload video: {}", userEmail, request.getTitle());
        
        VideoInitUploadResponse response = videoService.initUpload(userEmail, request);
        
        return ApiResponse.<VideoInitUploadResponse>builder()
                .result(response)
                .message("Upload initialized successfully")
                .build();
    }

    @Operation(summary = "Complete upload by filename", description = "Hoàn thành upload bằng cách tìm file từ DoodStream dựa trên filename")
    @PostMapping("/{videoId}/complete-upload-by-filename")
    public ApiResponse<VideoResponse> completeUploadByFilename(
            @PathVariable String videoId,
            @RequestParam String filename
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        log.info("🏁 User {} đang complete upload by filename: {} - filename: {}", userEmail, videoId, filename);
        
        VideoResponse response = videoService.completeUploadByFilename(userEmail, videoId, filename);
        
        return ApiResponse.<VideoResponse>builder()
                .result(response)
                .message("Upload completed successfully")
                .build();
    }

    @Operation(summary = "Complete upload", description = "Hoàn thành upload - cập nhật video với fileCode từ DoodStream")
    @PostMapping("/{videoId}/complete-upload")
    public ApiResponse<VideoResponse> completeUpload(
            @PathVariable String videoId,
            @RequestParam String fileCode
    ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        log.info("🏁 User {} đang complete upload video: {} - fileCode: {}", userEmail, videoId, fileCode);
        
        VideoResponse response = videoService.completeUpload(userEmail, videoId, fileCode);
        
        return ApiResponse.<VideoResponse>builder()
                .result(response)
                .message("Upload completed successfully")
                .build();
    }

    @Operation(summary = "Upload video", description = "Upload video lên DoodStream (legacy)")
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

    @Operation(summary = "Get my videos", description = "Lấy danh sách video của tôi (không bao gồm đã xóa)")
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
    
    @Operation(summary = "Get deleted videos", description = "Lấy danh sách video đã xóa (thùng rác) - Chỉ Admin")
    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<VideoResponse>> getDeletedVideos(
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
        
        Page<VideoResponse> videos = videoService.getDeletedVideos(userEmail, pageable);
        
        return ApiResponse.<Page<VideoResponse>>builder()
                .result(videos)
                .build();
    }
    
    @Operation(summary = "Restore video", description = "Khôi phục video đã xóa - Chỉ Admin")
    @PostMapping("/{videoId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VideoResponse> restoreVideo(@PathVariable String videoId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        VideoResponse video = videoService.restoreVideo(userEmail, videoId);
        
        return ApiResponse.<VideoResponse>builder()
                .result(video)
                .message("Khôi phục video thành công")
                .build();
    }

    @Operation(summary = "Get public videos", description = "Lấy danh sách video công khai")
    @GetMapping("/public")
    public ApiResponse<Page<VideoResponse>> getPublicVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<VideoResponse> videos = videoService.getPublicVideos(pageable, sort);
        
        return ApiResponse.<Page<VideoResponse>>builder()
                .result(videos)
                .build();
    }

    @Operation(summary = "Get all videos", description = "Lấy danh sách tất cả video (công khai và không công khai)")
    @GetMapping("/all")
    public ApiResponse<Page<VideoResponse>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<VideoResponse> videos = videoService.getAllVideos(pageable, sort);
        
        return ApiResponse.<Page<VideoResponse>>builder()
                .result(videos)
                .build();
    }

    @Operation(summary = "Get video by ID", description = "Lấy chi tiết video")
    @GetMapping("/{videoId}")
    public ApiResponse<VideoResponse> getVideoById(@PathVariable String videoId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        // Nếu là anonymousUser thì set null
        if ("anonymousUser".equals(userEmail)) {
            userEmail = null;
        }
        VideoResponse video = videoService.getVideoById(videoId, userEmail);
        
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
    @PreAuthorize("permitAll()")
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

    @Operation(summary = "Get direct video URL from DoodStream", description = "Lấy direct video URL để tránh CORS ở frontend")
    @GetMapping("/{fileCode}/stream-url")
    public ApiResponse<String> getVideoStreamUrl(@PathVariable String fileCode) {
        log.info("🎬 Đang lấy video URL cho fileCode: {}", fileCode);
        
        try {
            // Bước 1: Lấy trang embed để tìm pass_md5 URL
            String embedUrl = "https://dood.to/e/" + fileCode;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> embedResponse = getRestTemplate().exchange(
                embedUrl, HttpMethod.GET, entity, String.class);
            String html = embedResponse.getBody();
            
            if (html == null) {
                return ApiResponse.<String>builder()
                        .message("Không thể lấy trang embed")
                        .build();
            }
            
            // Parse pass_md5 URL từ HTML
            Pattern passMd5Pattern = Pattern.compile("/pass_md5/([^'\"]+)");
            Matcher passMd5Matcher = passMd5Pattern.matcher(html);
            
            String baseVideoUrl = null;
            if (passMd5Matcher.find()) {
                String passMd5Path = passMd5Matcher.group(1);
                String passMd5Url = "https://dood.to/pass_md5/" + passMd5Path;
                
                log.info("🔑 Gọi pass_md5 URL: {}", passMd5Url);
                
                // Bước 2: Gọi pass_md5 endpoint để lấy base URL
                ResponseEntity<String> passMd5Response = getRestTemplate().exchange(
                    passMd5Url, HttpMethod.GET, entity, String.class);
                baseVideoUrl = passMd5Response.getBody();
                
                log.info("📹 Base URL nhận được: {}", baseVideoUrl != null ? baseVideoUrl.substring(0, Math.min(50, baseVideoUrl.length())) + "..." : "null");
            }
            
            if (baseVideoUrl == null || baseVideoUrl.isEmpty() || baseVideoUrl.equals("RELOAD")) {
                return ApiResponse.<String>builder()
                        .message("Không thể lấy video URL từ DoodStream")
                        .build();
            }
            
            // Bước 3: Generate token và tạo final URL
            String token = generateRandomToken(10);
            String expiry = String.valueOf(System.currentTimeMillis());
            String finalVideoUrl = baseVideoUrl + token + "?token=" + token + "&expiry=" + expiry;
            
            log.info("✅ Final video URL: {}", finalVideoUrl.substring(0, Math.min(50, finalVideoUrl.length())) + "...");
            
            return ApiResponse.<String>builder()
                    .result(finalVideoUrl)
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy video URL: {}", e.getMessage(), e);
            return ApiResponse.<String>builder()
                    .message("Lỗi: " + e.getMessage())
                    .build();
        }
    }
    
    private String generateRandomToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Operation(summary = "Get direct video URL from Streamtape", description = "Lấy direct video URL (mp4) từ Streamtape để tránh CORS ở frontend, tương tự DoodStream stream-url")
    @GetMapping("/streamtape/stream-url")
    public ApiResponse<String> getStreamtapeStreamUrl(@RequestParam String url) {
        log.info("🎬 Đang tách direct video URL Streamtape từ: {}", url);

        String fileId = extractStreamtapeFileId(url);
        if (fileId == null) {
            return ApiResponse.<String>builder()
                    .message("URL Streamtape không hợp lệ")
                    .result("")
                    .build();
        }

        String directUrl = streamtapeService.getDirectVideoUrl(fileId);
        if (directUrl == null || directUrl.isEmpty()) {
            return ApiResponse.<String>builder()
                    .message("Không lấy được direct URL từ Streamtape (có thể do hạn chế IP/geo của server)")
                    .result("")
                    .build();
        }

        return ApiResponse.<String>builder()
                .result(directUrl)
                .build();
    }

    /**
     * Trích xuất file code Streamtape từ URL (hỗ trợ /e/, /v/, /embed/, hoặc chỉ mã)
     */
    private String extractStreamtapeFileId(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        Matcher m = Pattern.compile("streamtape\\.com/(?:e|v|embed)/([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE)
                .matcher(input);
        if (m.find()) {
            return m.group(1);
        }
        // Chỉ là mã thuần
        if (input.matches("[A-Za-z0-9]+")) {
            return input;
        }
        return null;
    }

    @Operation(summary = "Get related videos", description = "Lấy danh sách video liên quan")
    @GetMapping("/{videoId}/related")
    public ApiResponse<Page<VideoResponse>> getRelatedVideos(
            @PathVariable String videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        // Cho phép người dùng không đăng nhập xem video liên quan
        Page<VideoResponse> videos = videoService.getRelatedVideos(videoId, pageable);

        return ApiResponse.<Page<VideoResponse>>builder()
                .result(videos)
                .build();
    }
}
