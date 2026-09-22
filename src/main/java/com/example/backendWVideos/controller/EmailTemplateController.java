package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.EmailTemplateUpdateRequest;
import com.example.backendWVideos.dto.response.EmailTemplateResponse;
import com.example.backendWVideos.service.EmailTemplateService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/email-templates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailTemplateController {

    final EmailTemplateService emailTemplateService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<EmailTemplateResponse>> getAll() {
        return ApiResponse.<List<EmailTemplateResponse>>builder()
                .result(emailTemplateService.getAllForAdmin())
                .build();
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EmailTemplateResponse> update(
            @PathVariable String key,
            @Valid @RequestBody EmailTemplateUpdateRequest request) {
        return ApiResponse.<EmailTemplateResponse>builder()
                .message("Cập nhật email template thành công")
                .result(emailTemplateService.update(key, request))
                .build();
    }

    @PostMapping("/{key}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EmailTemplateResponse> reset(@PathVariable String key) {
        return ApiResponse.<EmailTemplateResponse>builder()
                .message("Đã khôi phục email template về mặc định")
                .result(emailTemplateService.reset(key))
                .build();
    }
}
