package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NavItemCreateRequest {
    
    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 100, message = "Tên hiển thị không được vượt quá 100 ký tự")
    private String label;
    
    @NotBlank(message = "Slug không được để trống")
    @Size(max = 100, message = "Slug không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang")
    private String slug;
    
    @NotBlank(message = "Đường dẫn không được để trống")
    @Size(max = 255, message = "Đường dẫn không được vượt quá 255 ký tự")
    private String href;
    
    @Size(max = 50, message = "Icon không được vượt quá 50 ký tự")
    private String icon;
    
    private Boolean isActive = true;
    
    private Boolean openNewTab = false;
    
    private Integer sortOrder = 0;
}
