package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Bản dịch tên danh mục theo 1 ngôn ngữ (dùng cho trang admin quản lý bản dịch).
 * name = null nghĩa là ngôn ngữ đó chưa có bản dịch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTranslationResponse {
    private String locale;
    private String name;
    private LocalDateTime updatedAt;
}
