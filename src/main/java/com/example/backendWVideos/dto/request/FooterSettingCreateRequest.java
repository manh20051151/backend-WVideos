package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FooterSettingCreateRequest {

    @NotBlank(message = "Khóa cấu hình không được để trống")
    @Size(max = 100, message = "Khóa cấu hình không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Khóa cấu hình chỉ được chứa chữ thường, số và dấu gạch dưới")
    private String settingKey;

    @Size(max = 5000, message = "Giá trị cấu hình không được vượt quá 5000 ký tự")
    private String settingValue;

    private Boolean isActive = true;
}