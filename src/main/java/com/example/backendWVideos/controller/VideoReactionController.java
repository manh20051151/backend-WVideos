package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.VideoReactionRequest;
import com.example.backendWVideos.dto.response.VideoReactionResponse;
import com.example.backendWVideos.enums.VideoReactionType;
import com.example.backendWVideos.service.VideoReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoReactionController {
    
    private final VideoReactionService videoReactionService;
    
    // Toggle like/dislike
    @PostMapping("/{videoId}/reactions")
    public ResponseEntity<ApiResponse<VideoReactionResponse>> toggleReaction(
            @PathVariable String videoId,
            @RequestBody VideoReactionRequest request) {
        return ResponseEntity.ok(videoReactionService.toggleReaction(videoId, request.getReactionType()));
    }
    
    // Lấy số lượng reactions
    @GetMapping("/{videoId}/reactions")
    public ResponseEntity<ApiResponse<VideoReactionResponse>> getReactionCounts(@PathVariable String videoId) {
        VideoReactionResponse response = videoReactionService.getReactionCounts(videoId);
        return ResponseEntity.ok(ApiResponse.<VideoReactionResponse>builder()
                .code(1000)
                .message("OK")
                .result(response)
                .build());
    }
}