package com.example.backendWVideos.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalResponse {
    String id;
    String userId;
    String userEmail;
    String userFullName;
    Double amount;
    String bankName;
    String bankAccountHolderName;
    String bankAccountNumber;
    String status;
    String adminNote;
    LocalDateTime createdAt;
    LocalDateTime processedAt;
}