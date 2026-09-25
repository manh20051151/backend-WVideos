package com.example.backendWVideos.exception;

import com.example.backendWVideos.dto.request.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Xử lý lỗi toàn cục. Message lỗi được i18n theo header Accept-Language:
 * key = "error.<Tên enum ErrorCode>" tra từ messages_<locale>.properties,
 * fallback về message tiếng Việt trong enum khi thiếu bản dịch.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";
    private static final String KEY_PREFIX = "error.";

    private final MessageSource messageSource;

    /**
     * Tra message theo ngôn ngữ của request; thiếu bản dịch thì dùng message vi trong enum.
     */
    private String resolveMessage(ErrorCode errorCode, Locale locale) {
        return messageSource.getMessage(KEY_PREFIX + errorCode.name(), null, errorCode.getMessage(), locale);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException exception, Locale locale){
        ErrorCode errorCode = exception.getErrorCode();

        String message = exception.getCustomMessage() != null
                ? exception.getCustomMessage()
                : resolveMessage(errorCode, locale);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(message);
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = ExpiredJwtException.class)
    ResponseEntity<ApiResponse> handlingExpiredJwtException(ExpiredJwtException exception, Locale locale){
        ErrorCode errorCode = ErrorCode.TOKEN_EXPIRED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(resolveMessage(errorCode, locale))
                        .build());
    }

    @ExceptionHandler(value = JwtException.class)
    ResponseEntity<ApiResponse> handlingJwtException(JwtException exception, Locale locale){
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(resolveMessage(errorCode, locale))
                        .build());
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException exception, Locale locale){
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(resolveMessage(errorCode, locale))
                        .build());
    }

    /**
     * Lỗi validation: DTO dùng tên enum làm message (vd "PASSWORD_INVALID") -> tra bundle i18n,
     * thay placeholder {min} bằng giá trị thật của constraint.
     * Message không phải tên enum (text thô) -> giữ nguyên text gốc, không che bằng INVALID_KEY.
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingValidation(MethodArgumentNotValidException exception, Locale locale){
        String defaultMessage = exception.getFieldError() != null
                ? exception.getFieldError().getDefaultMessage()
                : null;
        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        Map<String, Object> attributes = null;
        try {
            errorCode = ErrorCode.valueOf(defaultMessage);

            var constraintViolation = exception.getBindingResult()
                    .getAllErrors().get(0).unwrap(ConstraintViolation.class);
            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

        } catch (IllegalArgumentException | NullPointerException e){
            // Message không phải tên enum -> trả nguyên văn message validation
            ApiResponse rawResponse = new ApiResponse();
            rawResponse.setCode(ErrorCode.INVALID_KEY.getCode());
            rawResponse.setMessage(defaultMessage);
            return ResponseEntity.badRequest().body(rawResponse);
        }

        String template = resolveMessage(errorCode, locale);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(Objects.nonNull(attributes) ?
                mapAttribute(template, attributes)
                : template);

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse> handlingMaxUploadSizeExceededException(MaxUploadSizeExceededException exception, Locale locale){
        ErrorCode errorCode = ErrorCode.FILE_TOO_LARGE;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(resolveMessage(errorCode, locale))
                        .build());
    }

    private String mapAttribute(String message, Map<String, Object> attributes){
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
