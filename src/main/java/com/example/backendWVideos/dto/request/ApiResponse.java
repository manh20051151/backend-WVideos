package com.example.backendWVideos.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = 'API Response wrapper cho tất cả các endpoint')
public class ApiResponse <T> {
    @Builder.Default
    @Schema(description = 'Mã response code', example = '1000')
    int code = 1000;
    
    @Schema(description = 'Thông báo kết quả', example = 'Success')
    String message;
    
    @Schema(description = 'Dữ liệu trả về')
    T result;
}
