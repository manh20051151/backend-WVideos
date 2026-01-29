package com.example.backendWVideos.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    String password;
    String numberPhone;
    String fullName;
    LocalDate dob;
    List<String> roles;

    String avatar; // URL từ Cloudinary
    String email;
    Boolean gender; // True là nam
    
    // Thông tin ngân hàng
    String bankName; // Tên ngân hàng
    String bankAccountHolderName; // Tên chủ tài khoản ngân hàng
    String bankAccountNumber; // Số tài khoản ngân hàng
}
