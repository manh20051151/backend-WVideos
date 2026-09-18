package com.example.backendWVideos.dto.request;

import com.example.backendWVideos.enums.FooterSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FooterLinkUpdateRequest {

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 100, message = "Tên hiển thị không được vượt quá 100 ký tự")
    private String label;

    @NotBlank(message = "Đường dẫn không được để trống")
    @Size(max = 255, message = "Đường dẫn không được vượt quá 255 ký tự")
    private String href;

    @NotNull(message = "Khu vực hiển thị không được để trống")
    private FooterSection section;

    @Size(max = 500, message = "Icon không được vượt quá 500 ký tự")
    private String icon;

    private Boolean isActive;

    private Boolean openNewTab;

    private Integer sortOrder;
}