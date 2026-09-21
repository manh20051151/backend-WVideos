package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tạo / cập nhật lý do báo cáo (admin)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportReasonRequest {
    // Chỉ dùng khi tạo mới - mã viết hoa, không khoảng trắng (VD: SPAM, COPYRIGHT)
    @Pattern(regexp = "^[A-Z0-9_]{2,50}$", message = "Mã lý do chỉ gồm chữ hoa, số và dấu gạch dưới (2-50 ký tự)")
    private String code;

    @NotBlank(message = "Nhãn hiển thị không được để trống")
    @Size(max = 255, message = "Nhãn hiển thị tối đa 255 ký tự")
    private String label;

    @Size(max = 2000, message = "Đường dẫn icon tối đa 2000 ký tự")
    private String icon; // SVG path data (tùy chọn)

    private Integer sortOrder; // Thứ tự hiển thị

    private Boolean isActive; // Kích hoạt / ẩn
}
