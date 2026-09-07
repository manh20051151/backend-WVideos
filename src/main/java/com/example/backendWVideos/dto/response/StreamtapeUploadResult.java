package com.example.backendWVideos.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamtapeUploadResult {

    @JsonProperty("file_id")
    private String fileId;

    private String title;

    private String size;

    private String thumbnailUrl;

    private String embedUrl;

    private String downloadUrl;

    private Boolean converted;

    private Integer status;

    private Long duration;
}
