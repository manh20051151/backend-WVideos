package com.example.backendWVideos.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoodStreamUploadResult {
    
    @JsonProperty("download_url")
    private String downloadUrl;
    
    @JsonProperty("single_img")
    private String singleImg;
    
    private Integer status;
    
    @JsonProperty("filecode")
    private String fileCode;
    
    @JsonProperty("splash_img")
    private String splashImg;
    
    @JsonProperty("canplay")
    private Integer canPlay;
    
    private String size;
    
    private String length;
    
    private String uploaded;
    
    @JsonProperty("protected_embed")
    private String protectedEmbed;
    
    @JsonProperty("protected_dl")
    private String protectedDl;
    
    private String title;
}
