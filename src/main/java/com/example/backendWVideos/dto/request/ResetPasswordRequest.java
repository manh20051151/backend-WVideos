package com.example.backendWVideos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {

    @NotBlank(message = "Token không hợp lệ")
    String token;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "PASSWORD_INVALID")
    String password;
}
