package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.enums.FooterSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FooterLinkResponse {

    private String id;
    private String label;
    private String href;
    private FooterSection section;
    private String icon;
    private Boolean isActive;
    private Boolean openNewTab;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByName; // Tên đầy đủ người tạo
}