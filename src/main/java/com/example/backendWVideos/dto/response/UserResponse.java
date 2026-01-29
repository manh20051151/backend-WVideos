package com.example.backendWVideos.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String username;
    String numberPhone;
    String fullName;
    LocalDate dob;
    Set<RoleResponse> roles;

    String avatar; // URL từ Cloudinary
    String email;
    Boolean gender; // True là nam

    boolean locked;
    Date lockedAt;
    String lockReason;
    Date joinedDate;
    
    // Thông tin ngân hàng
    String bankName; // Tên ngân hàng
    String bankAccountHolderName; // Tên chủ tài khoản ngân hàng
    String bankAccountNumber; // Số tài khoản ngân hàng
    
    // Danh sách ID tài liệu người dùng đã mua
    List<String> purchasedDocumentIds;
}
