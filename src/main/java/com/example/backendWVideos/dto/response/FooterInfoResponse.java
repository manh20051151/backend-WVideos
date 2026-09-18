package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.enums.FooterSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FooterInfoResponse {

    // Link của footer nhóm theo section (QUICK_LINKS, CATEGORIES, SUPPORT, SOCIAL, BOTTOM)
    private Map<FooterSection, List<FooterLinkResponse>> links;

    // Các cấu hình đang hoạt động (brand_description, copyright_text...)
    private Map<String, String> settings;
}