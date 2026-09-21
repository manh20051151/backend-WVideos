package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Báo cáo vi phạm video. `reason` là code của lý do báo cáo (admin CRUD được danh sách lý do).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoReportRequest {
    @NotBlank(message = "Vui lòng chọn lý do báo cáo")
    @Size(max = 50, message = "Mã lý do tối đa 50 ký tự")
    private String reason; // Code lý do (tham chiếu report_reasons.code)

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description; // Mô tả chi tiết (tùy chọn)
}
