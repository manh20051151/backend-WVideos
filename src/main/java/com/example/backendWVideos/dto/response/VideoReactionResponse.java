package com.example.backendWVideos.dto.response;

import com.example.backendWVideos.enums.VideoReactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoReactionResponse {
    Long likeCount;
    Long dislikeCount;
    VideoReactionType userReaction; // LIKE, DISLIKE, hoặc null
}