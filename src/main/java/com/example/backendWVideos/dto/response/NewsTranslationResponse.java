package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Bản dịch bài tin tức theo 1 ngôn ngữ (dùng cho trang admin quản lý bản dịch).
 * Các trường null = ngôn ngữ đó chưa có bản dịch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsTranslationResponse {
    private String locale;
    private String title;
    private String summary;
    private String content;
    private LocalDateTime updatedAt;
}
