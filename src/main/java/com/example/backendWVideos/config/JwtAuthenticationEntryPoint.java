package com.example.backendWVideos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ErrorCode errorCode = isTokenExpired(authException)
                ? ErrorCode.TOKEN_EXPIRED
                : ErrorCode.UNAUTHENTICATED;

        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }

    /**
     * Kiểm tra toàn bộ chuỗi cause xem token có hết hạn không.
     * Spring Security có thể wrap lỗi expired trong InvalidBearerTokenException/
     * BadCredentialsException với message chung ("Invalid bearer token", ...)
     * KHÔNG chứa chữ "expired", nên không thể chỉ dựa vào message ngoài cùng.
     */
    private boolean isTokenExpired(AuthenticationException authException) {
        Throwable cause = authException;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("expired") || lower.contains("hết hạn") || lower.contains("expires")) {
                    return true;
                }
            }
            if (cause.getClass().getSimpleName().contains("Expired")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
