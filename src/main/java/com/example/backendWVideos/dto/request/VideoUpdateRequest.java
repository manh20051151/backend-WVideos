package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

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
    
    // Danh sách ID của các thể loại (yêu cầu ít nhất 1, tối đa 10 nếu có)
    @Size(min = 1, max = 10, message = "Phải chọn từ 1 đến 10 thể loại")
    private List<String> categoryIds;
}
