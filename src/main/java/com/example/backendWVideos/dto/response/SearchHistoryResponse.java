package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mục lịch sử tìm kiếm - dùng cho gợi ý "tìm kiếm gần đây" trên header.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryResponse {
    private Long id;
    private String query;
    private Long searchCount;
    private LocalDateTime searchedAt;
}
