package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.SiteSettingUpdateRequest;
import com.example.backendWVideos.dto.response.SiteSettingResponse;
import com.example.backendWVideos.service.SiteSettingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteSettingController {

    final SiteSettingService siteSettingService;

    /**
     * Public: FE lấy logo/favicon để hiển thị.
     */
    @GetMapping("/site-settings")
    public ApiResponse<Map<String, String>> getPublicSettings() {
        return ApiResponse.<Map<String, String>>builder()
                .result(siteSettingService.getPublicSettings())
                .build();
    }

    @GetMapping("/admin/site-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SiteSettingResponse>> getAllForAdmin() {
        return ApiResponse.<List<SiteSettingResponse>>builder()
                .result(siteSettingService.getAllForAdmin())
                .build();
    }

    @PutMapping("/admin/site-settings/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SiteSettingResponse> update(
            @PathVariable String key,
            @RequestBody SiteSettingUpdateRequest request) {
        return ApiResponse.<SiteSettingResponse>builder()
                .message("Cập nhật cấu hình thành công")
                .result(siteSettingService.update(key, request.getValue()))
                .build();
    }
}
