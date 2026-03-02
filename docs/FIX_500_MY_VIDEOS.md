# Fix lỗi 500 khi gọi /videos/my-videos

## Vấn đề
Khi gọi API `GET /videos/my-videos`, backend trả về lỗi 500.

## Nguyên nhân
1. **LazyInitializationException**: Entity `Video` có `@ManyToOne(fetch = FetchType.LAZY)` cho `user`, nhưng khi map sang DTO bên ngoài transaction context, không thể access `user.username`.

2. **Null username**: User đăng ký bằng OAuth2 (Google) có thể không có `username` (chỉ có email), dẫn đến null khi mapping.

## Giải pháp đã áp dụng

### 1. Thêm @Transactional(readOnly = true) cho các read methods
File: `VideoService.java`

```java
@Transactional(readOnly = true)
public Page<VideoResponse> getMyVideos(String userEmail, Pageable pageable) {
    // ...
}

@Transactional(readOnly = true)
public Page<VideoResponse> getPublicVideos(Pageable pageable) {
    // ...
}

@Transactional(readOnly = true)
public VideoResponse getVideoById(String videoId) {
    // ...
}
```

Điều này đảm bảo lazy-loaded relationships có thể được access trong transaction context.

### 2. Sửa VideoMapper để handle null username
File: `VideoMapper.java`

```java
@Mapper(componentModel = "spring")
public interface VideoMapper {
    
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "video", target = "username", qualifiedByName = "getUsername")
    VideoResponse toVideoResponse(Video video);
    
    @Named("getUsername")
    default String getUsername(Video video) {
        if (video.getUser() == null) {
            return null;
        }
        // Nếu username null, dùng email làm fallback
        String username = video.getUser().getUsername();
        return username != null ? username : video.getUser().getEmail();
    }
}
```

Nếu `username` null, sẽ dùng `email` làm fallback.

## Cách test

### 1. Restart backend
```bash
# Stop backend nếu đang chạy
# Ctrl+C

# Start lại
mvn spring-boot:run
```

### 2. Test với Postman/curl
```bash
# Login để lấy token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nguyenvietmanh1409@gmail.com",
    "password": "your_password"
  }'

# Lấy danh sách video của user
curl -X GET "http://localhost:8080/api/videos/my-videos?page=0&size=10" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 3. Test trên frontend
1. Đăng nhập vào http://localhost:3000
2. Vào trang "Video của tôi" (My Videos)
3. Kiểm tra xem có hiển thị danh sách video không

## Kết quả mong đợi
- API trả về status 200
- Response có dạng:
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "content": [
      {
        "id": "video-id",
        "title": "Video title",
        "username": "user@example.com",
        ...
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

## Commit message
```
fix: sửa lỗi 500 khi gọi /videos/my-videos

- Thêm @Transactional(readOnly = true) cho getMyVideos, getPublicVideos, getVideoById
- Sửa VideoMapper để handle null username (dùng email làm fallback)
- Fix LazyInitializationException khi access user.username
```
