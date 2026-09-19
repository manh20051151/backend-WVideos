package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kết quả gợi ý tìm kiếm nhanh trên header: gộp video + kênh + tin tức.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestResponse {
    private List<VideoResponse> videos;
    private List<ChannelSearchResult> channels;
    private List<NewsResponse> news;
}
