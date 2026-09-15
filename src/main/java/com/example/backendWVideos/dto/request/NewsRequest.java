package com.example.backendWVideos.dto.request;

import com.example.backendWVideos.enums.NewsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewsRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "Slug không được để trống")
    @Size(max = 200, message = "Slug không được vượt quá 200 ký tự")
    private String slug;

    @Size(max = 500, message = "Tóm tắt không được vượt quá 500 ký tự")
    private String summary;

    // Nội dung HTML (có thể style) - cho phép null với bản nháp
    private String content;

    private String thumbnailUrl;

    private String categoryId;

    private NewsStatus status = NewsStatus.DRAFT;
}
