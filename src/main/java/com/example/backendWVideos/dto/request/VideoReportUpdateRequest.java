package com.example.backendWVideos.dto.request;

import com.example.backendWVideos.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin cập nhật trạng thái xử lý báo cáo video
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoReportUpdateRequest {
    @NotNull(message = "Thiếu trạng thái xử lý")
    private ReportStatus status; // RESOLVED hoặc DISMISSED

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String adminNote; // Ghi chú khi xử lý (tùy chọn)
}
