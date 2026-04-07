package com.example.backendWVideos.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFinancialInfoDTO {
    String userId;
    Double balance;
    Double revenue;
    String message;
}
