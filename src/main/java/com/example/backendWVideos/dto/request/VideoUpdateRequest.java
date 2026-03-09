package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUpdateRequest {
    
    @Size(min = 3, max = 200, message = "Tiêu đề phải từ 3-200 ký tự")
    private String title;
    
    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;
    
    private Boolean isPublic;
    
    // ID của thể loại (có thể null để xóa thể loại)
    private String categoryId;
}
