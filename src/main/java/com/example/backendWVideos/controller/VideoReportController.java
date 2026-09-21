package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.VideoReportRequest;
import com.example.backendWVideos.dto.request.VideoReportUpdateRequest;
import com.example.backendWVideos.dto.response.VideoReportResponse;
import com.example.backendWVideos.enums.ReportStatus;
import com.example.backendWVideos.service.VideoReportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API báo cáo vi phạm video: user gửi báo cáo, admin quản lý/xử lý.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class VideoReportController {

    private final VideoReportService videoReportService;

    @Operation(summary = "Report video", description = "User báo cáo vi phạm video (mỗi user 1 lần/video, không báo cáo được video của chính mình). Yêu cầu đăng nhập")
    @PostMapping("/video/{videoId}")
    public ApiResponse<VideoReportResponse> reportVideo(
            @PathVariable String videoId,
            @Valid @RequestBody VideoReportRequest request
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<VideoReportResponse>builder()
                .result(videoReportService.createReport(email, videoId, request))
                .message("Đã gửi báo cáo, quản trị viên sẽ xem xét sớm")
                .build();
    }

    @Operation(summary = "Get reports (admin)", description = "Danh sách báo cáo video, lọc theo trạng thái (PENDING/RESOLVED/DISMISSED)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<VideoReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ApiResponse.<Page<VideoReportResponse>>builder()
                .result(videoReportService.getReports(status, pageable))
                .build();
    }

    @Operation(summary = "Get my reports", description = "Danh sách báo cáo của user hiện tại. Yêu cầu đăng nhập")
    @GetMapping("/my")
    public ApiResponse<Page<VideoReportResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ApiResponse.<Page<VideoReportResponse>>builder()
                .result(videoReportService.getMyReports(email, pageable))
                .build();
    }

    @Operation(summary = "Withdraw report", description = "Rút lại báo cáo đang chờ xử lý (chỉ báo cáo của chính mình)")
    @DeleteMapping("/{reportId}")
    public ApiResponse<Void> withdrawReport(@PathVariable Long reportId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        videoReportService.withdrawReport(email, reportId);
        return ApiResponse.<Void>builder()
                .message("Đã rút báo cáo")
                .build();
    }

    @Operation(summary = "Get report counts (admin)", description = "Số báo cáo theo từng trạng thái (cho badge tab)")
    @GetMapping("/counts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Long>> getCounts() {
        return ApiResponse.<Map<String, Long>>builder()
                .result(videoReportService.getCounts())
                .build();
    }

    @Operation(summary = "Update report status (admin)", description = "Admin xử lý báo cáo: đánh dấu RESOLVED (đã xử lý) hoặc DISMISSED (bỏ qua)")
    @PutMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VideoReportResponse> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody VideoReportUpdateRequest request
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<VideoReportResponse>builder()
                .result(videoReportService.updateStatus(email, reportId, request))
                .message("Đã cập nhật trạng thái báo cáo")
                .build();
    }
}
