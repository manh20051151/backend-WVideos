package com.example.backendWVideos.dto.request;

import com.example.backendWVideos.enums.VideoReactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoReactionRequest {
    VideoReactionType reactionType;
}