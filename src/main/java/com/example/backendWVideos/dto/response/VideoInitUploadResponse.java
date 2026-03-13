package com.example.backendWVideos.dto.response;

import lombok.Data;

@Data
public class VideoInitUploadResponse {
    private String videoId;
    private String uploadServerUrl;
    private String uploadToken;
    private String status;
}
