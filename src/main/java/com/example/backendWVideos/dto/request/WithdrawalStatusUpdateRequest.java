package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalStatusUpdateRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    String status; // APPROVED | REJECTED

    String adminNote;
}