package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentBanRequest {

    // Số giờ khóa bình luận (1 giờ -> 1 năm)
    @Min(value = 1, message = "Thời gian khóa tối thiểu 1 giờ")
    @Max(value = 8760, message = "Thời gian khóa tối đa 8760 giờ (1 năm)")
    private int hours = 24;

    // Lý do khóa
    @NotBlank(message = "Lý do khóa không được để trống")
    @Size(max = 255, message = "Lý do không được vượt quá 255 ký tự")
    private String reason;
}