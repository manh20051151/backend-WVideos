package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryUpdateRequest {
    
    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(max = 100, message = "Tên thể loại không được vượt quá 100 ký tự")
    private String name;
    
    @NotBlank(message = "Slug không được để trống")
    @Size(max = 100, message = "Slug không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang")
    private String slug;
    
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu sắc phải có định dạng hex (VD: #FF0000)")
    private String color;
    
    @Size(max = 50, message = "Icon không được vượt quá 50 ký tự")
    private String icon;
    
    private Boolean isActive;
    
    private Integer sortOrder;
}