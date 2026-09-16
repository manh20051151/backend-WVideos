package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalCreateRequest {

    @NotNull(message = "Số tiền rút không được để trống")
    @DecimalMin(value = "500000", message = "Số tiền rút tối thiểu là 500.000 VNĐ")
    Double amount;
}