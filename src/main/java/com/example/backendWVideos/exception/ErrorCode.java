package com.example.backendWVideos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999,"Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(9998,"Dịch vụ tạm thời không khả dụng", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001,"Khóa tin nhắn không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002,"Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003,"username có ít nhất {min}  ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004,"password có ít nhất {min}  ký tự", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005,"Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006,"Không xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007,"Không có quyền", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008,"'Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    USER_NOT_LOCKED(1009,"Tài khoản không bị khóa", HttpStatus.BAD_REQUEST),
    USER_LOCKED(10011,"Tài khoản bị khóa", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1010,"Không tìm thấy role", HttpStatus.BAD_REQUEST),


    USER_NOT_FOUND(2002, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),

    //NOTIFICATION
    NOTIFICATION_NOT_FOUND(4002, "Lỗi không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    EMAIL_NOT_MATCH(4003, "Email không khớp với tài khoản", HttpStatus.NOT_FOUND),
    INVALID_DATA(4004, "Data không hợp lệ", HttpStatus.NOT_FOUND),
    EMAIL_SENDING_FAILED(4005, "Gửi email thất bại", HttpStatus.NOT_FOUND),

    // Đăng ký
    INVALID_TOKEN(5001, "Token không hợp lệ", HttpStatus.NOT_FOUND),
    TOKEN_EXPIRED(5002, "Token đã hết hạn", HttpStatus.NOT_FOUND),
    ALREADY_CONFIRMED(5003, "Tài khoản đã được xác nhận", HttpStatus.NOT_FOUND),
    REGISTRATION_FAILED(5004, "Đăng ký thất bại", HttpStatus.NOT_FOUND),
    CONFIRMATION_FAILED(5005, "Xác nhận đăng ký thất bại", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(5006, "Email đã tồn tại", HttpStatus.NOT_FOUND),



    CATEGORY_NOT_FOUND(6001, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    CATEGORY_NAME_EXISTED(6002, "Tên danh mục đã tồn tại", HttpStatus.BAD_REQUEST),
    CATEGORY_LEVEL_EXCEEDED(6003, "Vượt quá số cấp cho phép", HttpStatus.BAD_REQUEST),
    CATEGORY_EXISTED(6003, "Vui lòng chọn danh mục cấp thấp nhất", HttpStatus.BAD_REQUEST),

    //school
    SCHOOL_NOT_FOUND(7001, "Không tìm thấy trường học",HttpStatus.BAD_REQUEST),
    SCHOOL_EXISTED(7002, "Tên trường đã tồn tại",HttpStatus.BAD_REQUEST),

    //document
    DOCUMENT_NOT_FOUND(8001, "Tài liệu không tồn tại",HttpStatus.BAD_REQUEST),
    CONCURRENT_MODIFICATION(8002, "Tài liệu đang được cập nhật bởi người khác, vui lòng thử lại sau", HttpStatus.CONFLICT),
    INVALID_STATUS(8003, "Trạng thái không tồn tại, vui lòng thử lại sau", HttpStatus.CONFLICT),
    INVALID_PAGE_COUNT(8004, "Lỗi dữ liệu số trang", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(8005, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    
    // Financial errors
    INSUFFICIENT_BALANCE(9001, "Số dư tài khoản không đủ", HttpStatus.BAD_REQUEST),
    INVALID_OPERATION(9002, "Phép toán không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT(9003, "Số tiền không hợp lệ", HttpStatus.BAD_REQUEST),
    
    // Document purchase errors
    DOCUMENT_NOT_APPROVED(9101, "Tài liệu chưa được phê duyệt", HttpStatus.BAD_REQUEST),
    CANNOT_PURCHASE_OWN_DOCUMENT(9102, "Không thể mua tài liệu của chính mình", HttpStatus.BAD_REQUEST),
    DOCUMENT_ALREADY_PURCHASED(9103, "Bạn đã mua tài liệu này rồi", HttpStatus.BAD_REQUEST),
    PURCHASE_FAILED(9104, "Giao dịch mua tài liệu thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    PURCHASE_NOT_FOUND(9105, "Không tìm thấy thông tin giao dịch mua", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(9106, "Không có quyền truy cập", HttpStatus.FORBIDDEN),
    
    // File validation errors
    FILE_NOT_FOUND(9201, "Không tìm thấy file", HttpStatus.BAD_REQUEST),
    INVALID_FILE_NAME(9202, "Tên file không hợp lệ", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(9203, "File quá lớn (tối đa 50MB)", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(9204, "Loại file không được hỗ trợ", HttpStatus.BAD_REQUEST),
    FILE_CORRUPTED(9205, "File bị lỗi hoặc không thể đọc", HttpStatus.BAD_REQUEST),
    
    // Withdrawal request errors
    WITHDRAWAL_REQUEST_NOT_FOUND(9301, "Không tìm thấy yêu cầu rút tiền", HttpStatus.NOT_FOUND),
    WITHDRAWAL_REQUEST_ALREADY_PROCESSED(9302, "Yêu cầu rút tiền đã được xử lý", HttpStatus.BAD_REQUEST),
    BANK_INFO_NOT_FOUND(9303, "Vui lòng cập nhật thông tin ngân hàng trước khi rút tiền", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_REVENUE(9304, "Doanh thu không đủ để rút tiền", HttpStatus.BAD_REQUEST),
    
    // Favorite errors
    FAVORITE_ALREADY_EXISTS(9401, "Tài liệu đã có trong danh sách yêu thích", HttpStatus.BAD_REQUEST),
    FAVORITE_NOT_FOUND(9402, "Không tìm thấy trong danh sách yêu thích", HttpStatus.NOT_FOUND),
    
    // Document report errors
    DOCUMENT_REPORT_ALREADY_EXISTS(9501, "Bạn đã báo cáo tài liệu này rồi. Mỗi người dùng chỉ có thể báo cáo một tài liệu một lần", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

}
