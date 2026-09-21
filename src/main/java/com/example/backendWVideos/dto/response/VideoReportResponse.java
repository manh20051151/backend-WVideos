package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Thông tin báo cáo video (dùng cho trang quản trị).
 * `reason` là code lý do - frontend tự map sang nhãn hiển thị qua /report-reasons.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoReportResponse {
    private Long id;
    private String videoId;
    private String videoTitle;       // null nếu video đã bị xóa
    private String videoSlug;
    private String videoThumbnailUrl;
    private String reporterName;     // Tên user báo cáo
    private String reporterEmail;
    private String reason;           // Code lý do báo cáo
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String adminNote;
}
