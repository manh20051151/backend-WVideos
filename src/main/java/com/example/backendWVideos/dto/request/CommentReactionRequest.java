package com.example.backendWVideos.dto.request;

import com.example.backendWVideos.enums.CommentReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentReactionRequest {

    @NotNull(message = "Loại phản ứng không được để trống")
    CommentReactionType reactionType; // LIKE | DISLIKE
}
