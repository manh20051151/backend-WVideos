package com.example.backendWVideos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Admin cập nhật bản dịch tên danh mục (thể loại video / danh mục tin tức).
 * name để trống -> xóa bản dịch của ngôn ngữ đó (fallback tên tiếng Việt gốc).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTranslationUpsertRequest {

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

        // Tên đã dịch; chuỗi rỗng/null -> xóa bản dịch
        private String name;
    }
}
