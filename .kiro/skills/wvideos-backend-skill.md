---
name: WVideos Backend Development
description: Skill để làm việc với WVideos Backend - Spring Boot API với JWT, OAuth2, DoodStream integration
---

# WVideos Backend Development Skill

## Mục Đích
Skill này hướng dẫn AI làm việc hiệu quả với dự án WVideos Backend - một REST API được xây dựng bằng Spring Boot 3.2.2, Java 17, tích hợp JWT authentication, OAuth2 Google, và DoodStream video storage.

## Tech Stack
- **Framework**: Spring Boot 3.2.2
- **Java**: 17
- **Database**: MySQL 8 (db_wvideos)
- **Cache**: Redis
- **Authentication**: JWT + OAuth2 (Google)
- **Video Storage**: DoodStream API
- **Payment**: Sepay Integration
- **Build Tool**: Maven

## Cấu Trúc Dự Án

```
backendWVideos/
├── src/main/java/com/example/backendWVideos/
│   ├── config/          # Security, Redis, Swagger, OAuth2
│   ├── controller/      # REST API Controllers (10 files)
│   ├── service/         # Business Logic (13 services)
│   ├── repository/      # JPA Repositories (12 files)
│   ├── entity/          # Database Entities (12 files)
│   ├── dto/
│   │   ├── request/     # Request DTOs
│   │   └── response/    # Response DTOs
│   ├── mapper/          # MapStruct Mappers
│   ├── security/        # Security Components
│   ├── exception/       # Exception Handling
│   ├── validator/       # Custom Validators
│   └── enums/           # Enumerations
├── src/main/resources/
│   ├── application.yaml # Cấu hình chính
│   └── templates/       # Email templates
└── docs/                # Documentation
```

## Commands

```bash
# Build project
.\mvnw.cmd clean install

# Run application
.\mvnw.cmd spring-boot:run

# Run tests
.\mvnw.cmd test
```

## Quy Tắc Khi Code

### 1. Naming Conventions
- **Classes**: PascalCase (e.g., `VideoService`, `UserController`)
- **Methods**: camelCase (e.g., `uploadVideo`, `getVideoById`)
- **Variables**: camelCase (e.g., `videoId`, `userEmail`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `JWT_VALID_DURATION`)
- **Packages**: lowercase (e.g., `com.example.backendWVideos.service`)

### 2. Logging
- Sử dụng SLF4J với emoji để dễ đọc:
  - 🚀 - Start operation
  - ✅ - Success
  - ❌ - Error
  - 🔍 - Debug/Info
  - 📤 - Upload
  - 📥 - Download
  - 🔄 - Sync/Refresh

```java
log.info("🚀 User {} đang upload video: {}", userEmail, videoTitle);
log.error("❌ Lỗi khi upload file: {}", e.getMessage());
log.info("✅ Upload thành công! FileCode: {}", fileCode);
```

### 3. Comment Bằng Tiếng Việt
```java
// Lấy thông tin user từ database
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

// Kiểm tra xem user có bị khóa không
if (user.isLocked()) {
    throw new AppException(ErrorCode.USER_LOCKED);
}
```

### 4. Service Layer Pattern
- **Controller**: Nhận request, validate, gọi service, trả response
- **Service**: Business logic, transaction management
- **Repository**: Data access, JPA queries

```java
@RestController
@RequestMapping("/videos")
public class VideoController {
    private final VideoService videoService;
    
    @PostMapping("/upload")
    public ApiResponse<VideoResponse> uploadVideo(@RequestBody VideoRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        VideoResponse response = videoService.uploadVideo(userEmail, request);
        return ApiResponse.<VideoResponse>builder()
            .result(response)
            .build();
    }
}
```

### 5. Exception Handling
- Sử dụng `AppException` với `ErrorCode` enum
- Centralized exception handling với `@ControllerAdvice`

```java
if (video == null) {
    throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
}
```

### 6. DTO Pattern
- Request DTOs: Validation với Jakarta Validation
- Response DTOs: MapStruct mapping từ Entity

```java
@Data
@Builder
public class VideoUploadRequest {
    @NotBlank(message = "Title không được để trống")
    private String title;
    
    private String description;
    
    @NotNull(message = "File không được null")
    private MultipartFile file;
}
```

### 7. Security Best Practices
- JWT token stored in Redis (blacklist support)
- Password encoding với BCrypt (strength 10)
- CORS properly configured
- Rate limiting trên sensitive endpoints
- Soft delete cho User (locked flag)

## Key Services

### 1. AuthenticationService
- JWT generation, validation, refresh
- OAuth2 Google integration
- Infinite token support

### 2. VideoService
- Video CRUD operations
- 2-step upload flow (init → complete)
- Sync từ DoodStream
- Views tracking

### 3. DoodStreamService
- Get upload server
- Upload file to DoodStream
- Get file info
- File list management

### 4. UserService
- User registration + email verification
- Profile management
- Lock/unlock accounts

### 5. SepayService
- Payment webhook handling
- Transaction processing

## API Response Format

Tất cả API responses đều wrap trong `ApiResponse<T>`:

```java
@Data
@Builder
public class ApiResponse<T> {
    private int code = 1000;
    private String message;
    private T result;
}
```

## Database Entities

### Video Entity
- UUID primary key
- DoodStream metadata (fileCode, urls, thumbnails)
- Status enum (UPLOADING, PROCESSING, ACTIVE, FAILED, DELETED)
- ManyToOne với User
- ManyToMany với Category
- ElementCollection tags

### User Entity
- UUID primary key
- Auth provider (GOOGLE/LOCAL)
- Financial info (balance, revenue, bank info)
- Locked status (soft delete)
- ManyToMany với Role

## Video Upload Flow

1. **Init Upload** (`POST /videos/init-upload`)
   - Tạo video record với status UPLOADING
   - Lấy upload server từ DoodStream
   - Trả về videoId + uploadUrl

2. **Frontend Upload**
   - Upload trực tiếp lên DoodStream server

3. **Complete Upload** (`POST /videos/{videoId}/complete-upload`)
   - Nhận fileCode từ frontend
   - Sync thông tin từ DoodStream
   - Update status thành ACTIVE

## Configuration

### application.yaml
- Database: MySQL connection
- Redis: Cache configuration
- JWT: Signer key, durations
- DoodStream: API key, base URL
- Sepay: Payment gateway config
- OAuth2: Google client credentials

### Environment Variables (.env)
```bash
DB_URL=jdbc:mysql://localhost:3306/db_wvideos
DB_USERNAME=root
DB_PASSWORD=sapassword
REDIS_HOST=localhost
JWT_SIGNER_KEY=your-secret-key
DOODSTREAM_API_KEY=your-api-key
```

## Testing

### Swagger UI
- URL: `http://localhost:8080/swagger-ui.html`
- Test tất cả endpoints
- JWT authentication support

### Default Admin Account
```
Email: admin@wvideos.com
Password: admin123
```

## Common Tasks

### Thêm Service Mới
1. Tạo interface trong `service/`
2. Implement service với `@Service` annotation
3. Inject dependencies qua constructor
4. Add logging với emoji
5. Handle exceptions với AppException

### Thêm Entity Mới
1. Tạo class trong `entity/` với `@Entity`
2. Add Lombok annotations (@Data, @Builder, etc.)
3. Define relationships (@ManyToOne, @OneToMany, etc.)
4. Tạo Repository interface
5. Tạo DTOs (request + response)
6. Tạo MapStruct mapper

### Thêm API Endpoint Mới
1. Tạo method trong Controller với mapping annotation
2. Add Swagger documentation (@Operation)
3. Validate request với @Valid
4. Get user từ SecurityContext nếu cần
5. Call service method
6. Return ApiResponse wrapper

## Troubleshooting

### Issue: Token expired
- Check JWT_VALID_DURATION trong .env
- Verify token refresh flow
- Check Redis connection

### Issue: DoodStream upload fails
- Verify DOODSTREAM_API_KEY
- Check upload server availability
- Review file size limits (2GB max)

### Issue: Database connection error
- Check MySQL service running
- Verify DB credentials in .env
- Check database exists (auto-create enabled)

## Best Practices

✅ Luôn validate input với Jakarta Validation
✅ Sử dụng transactions cho operations phức tạp
✅ Log đầy đủ với emoji cho dễ debug
✅ Handle exceptions properly với AppException
✅ Comment bằng Tiếng Việt
✅ Follow naming conventions
✅ Use DTOs thay vì expose entities
✅ Implement pagination cho list endpoints
✅ Add Swagger documentation cho mọi endpoint
✅ Test với Swagger UI trước khi commit

## Git Commit Messages

Sử dụng conventional commits với Tiếng Việt:

```bash
feat: thêm API upload video
fix: sửa lỗi token refresh
refactor: cải thiện video service
docs: cập nhật API documentation
style: format code theo convention
perf: tối ưu query database
```

## Resources

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [DoodStream API](https://doodstream.com/api-docs)
- [Project README](../README.md)
- [API Testing Guide](../docs/api/API_TESTING_GUIDE.md)
