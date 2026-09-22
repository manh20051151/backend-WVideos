package com.example.backendWVideos.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteSettingUpdateRequest {

    // URL ảnh (upload qua ImgBB ở FE). Chuỗi rỗng = xóa, quay về mặc định.
    String value;
}
