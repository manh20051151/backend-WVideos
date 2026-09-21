package com.example.backendWVideos.enums;

/**
 * Trạng thái xử lý báo cáo video
 */
public enum ReportStatus {
    PENDING,    // Chờ xử lý
    RESOLVED,   // Đã xử lý
    DISMISSED   // Đã bỏ qua (báo cáo không hợp lệ)
}
