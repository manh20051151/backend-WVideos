package com.example.backendWVideos.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateByUserRequest {
    String numberPhone;
    String fullName;
    LocalDate dob;
    String email;
    Boolean gender; // True là nam
    
    // Thông tin ngân hàng
    String bankName; // Tên ngân hàng
    String bankAccountHolderName; // Tên chủ tài khoản ngân hàng
    String bankAccountNumber; // Số tài khoản ngân hàng
}
