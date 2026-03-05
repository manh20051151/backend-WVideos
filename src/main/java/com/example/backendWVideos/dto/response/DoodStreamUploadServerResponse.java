package com.example.backendWVideos.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoodStreamUploadServerResponse {
    
    private String msg;
    
    @JsonProperty("server_time")
    private String serverTime;
    
    private Integer status;
    
    private String result; // Upload server URL
}
