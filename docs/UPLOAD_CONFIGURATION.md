# Cấu hình Upload File

## Tổng quan
Backend WVideos hỗ trợ upload video lên DoodStream với giới hạn file tối đa 2GB.

## Cấu hình Spring Boot

### application.yaml
```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 2GB        # Kích thước file tối đa
      max-request-size: 2GB     # Kích thước request tối đa
      file-size-threshold: 10MB # Ngưỡng để lưu file vào disk
```

### Giải thích các tham số:
- `max-file-size`: Kích thước tối đa của một file đơn lẻ
- `max-request-size`: Kích thước tối đa của toàn bộ request (bao gồm file + metadata)
- `file-size-threshold`: Kích thước ngưỡng để Spring bắt đầu lưu file vào disk thay vì memory

## Exception Handling

### GlobalExceptionHandler
Đã thêm handler cho `MaxUploadSizeExceededException`:

```java
@ExceptionHandler(value = MaxUploadSizeExceededException.class)
ResponseEntity<ApiResponse> handlingMaxUploadSizeExceededException(MaxUploadSizeExceededException exception){
    ErrorCode errorCode = ErrorCode.FILE_TOO_LARGE;
    
    return ResponseEntity
            .status(errorCode.getStatusCode())
            .body(ApiResponse.builder()
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .build());
}
```

### ErrorCode
```java
FILE_TOO_LARGE(9203, "File quá lớn (tối đa 2GB)", HttpStatus.BAD_REQUEST)
```

## Frontend Validation

### Validation trước khi upload:
```typescript
// Validate file type
if (!selectedFile.type.startsWith('video/')) {
  setError('Vui lòng chọn file video');
  return;
}

// Validate file size (max 2GB)
const maxSize = 2 * 1024 * 1024 * 1024; // 2GB
if (selectedFile.size > maxSize) {
  setError('File quá lớn. Kích thước tối đa là 2GB');
  return;
}
```

## Lưu ý

### Performance
- File lớn hơn 10MB sẽ được lưu vào disk thay vì memory để tránh OutOfMemoryError
- Upload file lớn có thể mất thời gian, cần có progress indicator cho user

### Network
- Đảm bảo timeout đủ lớn cho upload file lớn
- Có thể cần tăng timeout của Tomcat nếu upload file rất lớn:
```yaml
server:
  tomcat:
    connection-timeout: 600000 # 10 minutes
```

### DoodStream Limits
- DoodStream có giới hạn riêng về kích thước file
- Kiểm tra documentation của DoodStream để biết giới hạn chính xác
- API key: `550224nz0x44aku5y80cv5`

## Testing

### Test với file nhỏ (< 10MB)
```bash
curl -X POST http://localhost:8080/api/videos/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@small_video.mp4" \
  -F "title=Test Video" \
  -F "isPublic=true"
```

### Test với file lớn (> 2GB)
Sẽ nhận được response:
```json
{
  "code": 9203,
  "message": "File quá lớn (tối đa 2GB)"
}
```

## Troubleshooting

### Lỗi: MaxUploadSizeExceededException
- **Nguyên nhân**: File vượt quá giới hạn 2GB
- **Giải pháp**: Giảm kích thước file hoặc tăng giới hạn trong application.yaml

### Lỗi: Connection reset
- **Nguyên nhân**: Timeout khi upload file lớn
- **Giải pháp**: Tăng timeout của Tomcat hoặc reverse proxy (nginx)

### Lỗi: OutOfMemoryError
- **Nguyên nhân**: Quá nhiều file lớn được upload đồng thời
- **Giải pháp**: 
  - Giảm `file-size-threshold` để lưu file vào disk sớm hơn
  - Tăng heap size của JVM: `-Xmx2g`
  - Giới hạn số lượng concurrent uploads

## Best Practices

1. **Validation phía client**: Validate file size và type trước khi upload để tiết kiệm bandwidth
2. **Progress indicator**: Hiển thị progress bar cho user khi upload file lớn
3. **Chunked upload**: Với file rất lớn, cân nhắc implement chunked upload
4. **Compression**: Nén video trước khi upload nếu có thể
5. **Error handling**: Xử lý lỗi rõ ràng và thông báo cho user
