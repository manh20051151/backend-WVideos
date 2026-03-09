package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

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
    
    // Danh sách ID của các thể loại (yêu cầu ít nhất 1, tối đa 10)
    @NotEmpty(message = "Phải chọn ít nhất 1 thể loại")
    @Size(min = 1, max = 10, message = "Phải chọn từ 1 đến 10 thể loại")
    private List<String> categoryIds;
}
