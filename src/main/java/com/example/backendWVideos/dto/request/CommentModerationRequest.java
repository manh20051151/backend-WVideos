package com.example.backendWVideos.dto.request;

import com.example.backendWVideos.enums.CommentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentModerationRequest {
    
    @NotNull(message = "Status không được null")
    private CommentStatus status;
    
    // Bắt buộc nếu status = REJECTED
    private String rejectionReason;
    
    // Admin có thể edit content trước khi approve
    private String editedContent;
}
