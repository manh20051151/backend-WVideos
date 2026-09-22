package com.example.backendWVideos.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailTemplateResponse {

    String templateKey;

    String subject;

    String body;

    LocalDateTime updatedAt;

    String defaultSubject;

    String defaultBody;

    // true nếu admin đã chỉnh sửa (có row trong DB), false nếu đang dùng mặc định
    boolean customized;
}
