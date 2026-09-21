package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lý do báo cáo video (dùng cho modal báo cáo và trang quản trị CRUD)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportReasonResponse {
    private Long id;
    private String code;
    private String label;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
