package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BankInfoUpdateRequest {
    
    @NotBlank(message = "Tên ngân hàng không được để trống")
    @Size(max = 100, message = "Tên ngân hàng không được vượt quá 100 ký tự")
    String bankName;
    
    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(max = 100, message = "Tên chủ tài khoản không được vượt quá 100 ký tự")
    String bankAccountHolderName;
    
    @NotBlank(message = "Số tài khoản không được để trống")
    @Pattern(regexp = "^[0-9]{6,20}$", message = "Số tài khoản phải từ 6-20 chữ số")
    String bankAccountNumber;
}
