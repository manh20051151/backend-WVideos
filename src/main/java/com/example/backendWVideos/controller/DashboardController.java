package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.DashboardStatsResponse;
import com.example.backendWVideos.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    /**
     * Lấy thống kê dashboard (admin only)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        System.out.println("=== Dashboard stats endpoint called ===");
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.<DashboardStatsResponse>builder()
                .result(stats)
                .message("Lấy thống kê dashboard thành công")
                .build());
    }
}