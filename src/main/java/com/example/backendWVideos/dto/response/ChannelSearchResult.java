package com.example.backendWVideos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tìm kiếm kênh (user) - chỉ lộ thông tin công khai, không lộ email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSearchResult {
    private String id;
    private String fullName;
    private String avatar;
    private String channelSlug; // dùng cho link /channel/{slug}
}
