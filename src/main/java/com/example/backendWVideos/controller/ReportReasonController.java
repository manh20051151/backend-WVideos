package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.ReportReasonRequest;
import com.example.backendWVideos.dto.response.ReportReasonResponse;
import com.example.backendWVideos.service.ReportReasonService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API quản lý lý do báo cáo video: public lấy danh sách kích hoạt, admin CRUD.
 */
@RestController
@RequestMapping("/report-reasons")
@RequiredArgsConstructor
public class ReportReasonController {

    private final ReportReasonService reportReasonService;

    @Operation(summary = "Get active report reasons", description = "Danh sách lý do báo cáo đang kích hoạt (public, dùng cho modal báo cáo)")
    @GetMapping
    public ApiResponse<List<ReportReasonResponse>> getActiveReasons() {
        return ApiResponse.<List<ReportReasonResponse>>builder()
                .result(reportReasonService.getActiveReasons())
                .build();
    }

    @Operation(summary = "Get all report reasons (admin)", description = "Tất cả lý do báo cáo, kể cả đã ẩn")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<ReportReasonResponse>> getAllReasons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return ApiResponse.<Page<ReportReasonResponse>>builder()
                .result(reportReasonService.getAllReasons(pageable))
                .build();
    }

    @Operation(summary = "Create report reason (admin)", description = "Tạo lý do báo cáo mới (code unique, không đổi được sau khi tạo)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportReasonResponse> createReason(@Valid @RequestBody ReportReasonRequest request) {
        return ApiResponse.<ReportReasonResponse>builder()
                .result(reportReasonService.createReason(request))
                .message("Đã tạo lý do báo cáo")
                .build();
    }

    @Operation(summary = "Update report reason (admin)", description = "Cập nhật nhãn/icon/thứ tự/trạng thái kích hoạt")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportReasonResponse> updateReason(
            @PathVariable Long id,
            @Valid @RequestBody ReportReasonRequest request
    ) {
        return ApiResponse.<ReportReasonResponse>builder()
                .result(reportReasonService.updateReason(id, request))
                .message("Đã cập nhật lý do báo cáo")
                .build();
    }

    @Operation(summary = "Delete report reason (admin)", description = "Xóa lý do báo cáo - bị chặn nếu đã có báo cáo dùng lý do này (hãy ẩn thay vì xóa)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteReason(@PathVariable Long id) {
        reportReasonService.deleteReason(id);
        return ApiResponse.<Void>builder()
                .message("Đã xóa lý do báo cáo")
                .build();
    }
}
