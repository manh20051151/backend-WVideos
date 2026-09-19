package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lưu từ khóa vào lịch sử tìm kiếm.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryRequest {
    @NotBlank(message = "Từ khóa không được để trống")
    private String query;
}
