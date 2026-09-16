package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.WithdrawalCreateRequest;
import com.example.backendWVideos.dto.request.WithdrawalStatusUpdateRequest;
import com.example.backendWVideos.dto.response.WithdrawalResponse;
import com.example.backendWVideos.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/withdrawals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WithdrawalController {

    WithdrawalService withdrawalService;

    @PostMapping
    @Operation(summary = "Tạo yêu cầu rút tiền",
            description = "Yêu cầu doanh thu hiện tại tối thiểu 500.000 VNĐ, đã cập nhật thông tin ngân hàng và không còn yêu cầu nào chưa xử lý")
    public ApiResponse<WithdrawalResponse> createMyWithdrawal(
            @Valid @RequestBody WithdrawalCreateRequest request) {
        return ApiResponse.<WithdrawalResponse>builder()
                .code(1000)
                .message("Tạo yêu cầu rút tiền thành công")
                .result(withdrawalService.createMyWithdrawal(request))
                .build();
    }

    @GetMapping("/my")
    @Operation(summary = "Danh sách yêu cầu rút tiền của tôi")
    public ApiResponse<List<WithdrawalResponse>> getMyWithdrawals() {
        return ApiResponse.<List<WithdrawalResponse>>builder()
                .code(1000)
                .message("Lấy danh sách yêu cầu rút tiền thành công")
                .result(withdrawalService.getMyWithdrawals())
                .build();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách yêu cầu rút tiền (Admin)")
    public ApiResponse<Page<WithdrawalResponse>> getAllWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.<Page<WithdrawalResponse>>builder()
                .code(1000)
                .message("Lấy danh sách yêu cầu rút tiền thành công")
                .result(withdrawalService.getAllWithdrawals(page, size, status))
                .build();
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật trạng thái yêu cầu rút tiền (Admin)",
            description = "status: APPROVED hoặc REJECTED. Khi duyệt sẽ trừ doanh thu của người dùng")
    public ApiResponse<WithdrawalResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody WithdrawalStatusUpdateRequest request) {
        return ApiResponse.<WithdrawalResponse>builder()
                .code(1000)
                .message("Cập nhật trạng thái yêu cầu rút tiền thành công")
                .result(withdrawalService.updateStatus(id, request))
                .build();
    }
}