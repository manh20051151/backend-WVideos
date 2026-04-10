package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    
    @NotBlank(message = "Nội dung comment không được để trống")
    @Size(max = 500, message = "Nội dung comment không được vượt quá 500 ký tự")
    private String content;
}
