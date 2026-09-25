package com.example.backendWVideos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Admin cập nhật bản dịch bài tin tức (title/summary/content theo từng ngôn ngữ).
 * title để trống -> xóa bản dịch của ngôn ngữ đó (fallback nội dung tiếng Việt gốc).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsTranslationUpsertRequest {

    @Valid
    private List<Item> translations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        // Mã ngôn ngữ đích: en, zh-CN, ja, ko, hi, th, lo, km...
        @NotBlank
        private String locale;

        private String title;
        private String summary;
        private String content;
    }
}
