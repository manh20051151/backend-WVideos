package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailTemplateUpdateRequest {

    @NotBlank(message = "Tiêu đề email không được để trống")
    String subject;

    @NotBlank(message = "Nội dung email không được để trống")
    String body;
}
