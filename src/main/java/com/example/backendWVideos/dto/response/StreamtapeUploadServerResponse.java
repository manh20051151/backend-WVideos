package com.example.backendWVideos.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamtapeUploadServerResponse {

    private Integer status;

    private String msg;

    private UploadResult result;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResult {

        private String url;

        @JsonProperty("valid_until")
        private String validUntil;
    }
}
