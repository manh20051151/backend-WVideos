package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadRequest {
    
    @NotBlank(message = "Tiêu đề video không được để trống")
    @Size(min = 3, max = 200, message = "Tiêu đề phải từ 3-200 ký tự")
    private String title;
    
    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;
    
    private Boolean isPublic = true;
    
    // ID của thể loại (có thể null)
    private String categoryId;
}
