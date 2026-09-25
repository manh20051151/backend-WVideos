package com.example.backendWVideos.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException{

    private final ErrorCode errorCode;

    /**
     * Message tùy chỉnh do caller cung cấp - handler sẽ dùng nguyên văn,
     * không tra bundle i18n theo ErrorCode.
     */
    private final String customMessage;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }
}
