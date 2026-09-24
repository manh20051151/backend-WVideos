package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tag thịnh hành: tổng lượt xem của các video công khai chứa tag.
 * Dùng cho đám mây tags trên đầu trang chủ (tag càng nhiều view càng lớn).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagTrendingResponse {
    private String tag;
    private Long totalViews;
    private Long videoCount;
}
