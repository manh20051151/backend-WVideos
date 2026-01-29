# WVideos Backend API

Backend cho hệ thống WVideos với JWT Authentication và Swagger Documentation.

## Tech Stack

- **Java 17** + **Spring Boot 3.2.2**
- **Spring Security** + **OAuth2 Resource Server** (JWT)
- **Spring Data JPA** + **MySQL 8**
- **Lombok** + **MapStruct** (DTO mapping)
- **Swagger/OpenAPI** (springdoc-openapi-starter-webmvc-ui)
- **Resilience4j** (Circuit Breaker, Rate Limiter, Retry)
- **Spring Mail** (Email support)

## Cấu trúc Project

```
src/main/java/com/example/backendWVideos/
├── config/              # Spring Configuration
│   ├── SecurityConfig.java
│   ├── CustomJwtDecoder.java
│   ├── SwaggerConfig.java
│   ├── ApplicationInitConfig.java
│   └── ...
├── controller/          # REST Controllers
│   ├── AuthenticationController.java
│   ├── UserController.java
│   └── ...
├── dto/                 # Data Transfer Objects
│   ├── request/
│   │   ├── AuthenticationRequest.java
│   │   ├── UserCreateRequest.java
│   │   └── ...
│   └── response/
│       ├── AuthenticationResponse.java
│       ├── UserResponse.java
│       └── ...
├── entity/              # JPA Entities
│   ├── User.java
│   ├── Role.java
│   ├── Permission.java
│   └── InvalidatedToken.java
├── enums/               # Enumerations
│   └── AuthProvider.java
├── exception/           # Exception Handling
│   ├── AppException.java
│   ├── ErrorCode.java
│   └── GlobalExceptionHandler.java
├── mapper/              # MapStruct Mappers
│   └── UserMapper.java
├── repository/          # JPA Repositories
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   └── ...
├── security/            # Security Components
│   └── CurrentUser.java
├── service/             # Business Logic
│   ├── AuthenticationService.java
│   ├── UserService.java
│   └── ...
└── validator/           # Custom Validators
    └── DobValidator.java
```

## Cài đặt và Chạy

### 1. Yêu cầu

- Java 17+
- Maven 3.6+
- MySQL 8.0+

### 2. Tạo Database

```sql
CREATE DATABASE IF NOT EXISTS db_wvideos 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### 3. Cấu hình

Cập nhật file `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_wvideos
    username: root
    password: your_password
  
  mail:
    username: your-email@gmail.com
    password: your-app-password

jwt:
  signerKey: your-secret-key-at-least-32-characters-long
```

### 4. Build và Run

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

Hoặc chạy trực tiếp từ IDE (IntelliJ IDEA, Eclipse, VS Code).

### 5. Truy cập

- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/token` | Đăng nhập | ❌ |
| POST | `/auth/refresh` | Refresh token | ❌ |
| POST | `/auth/logout` | Đăng xuất | ❌ |
| POST | `/auth/introspect` | Kiểm tra token | ❌ |
| POST | `/auth/infinite-token` | Tạo token vô hạn (Admin) | ✅ |

### User Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/users/register` | Đăng ký tài khoản | ❌ |
| GET | `/users/confirm` | Xác nhận email | ❌ |
| GET | `/users/myInfo` | Lấy thông tin user hiện tại | ✅ |
| PUT | `/users/my-info` | Cập nhật thông tin | ✅ |
| PUT | `/users/change-password` | Đổi mật khẩu | ✅ |
| POST | `/users/forgot-password` | Quên mật khẩu | ❌ |
| GET | `/users` | Lấy danh sách users (Admin) | ✅ |
| POST | `/users/{id}/lock` | Khóa tài khoản (Admin) | ✅ |
| POST | `/users/{id}/unlock` | Mở khóa tài khoản (Admin) | ✅ |

## Sử dụng Swagger UI

### 1. Truy cập Swagger UI

Mở trình duyệt và truy cập: http://localhost:8080/swagger-ui.html

### 2. Xác thực với JWT Token

1. Đăng nhập để lấy token:
   - Mở endpoint `POST /auth/token`
   - Click "Try it out"
   - Nhập email và password
   - Click "Execute"
   - Copy token từ response

2. Authorize:
   - Click nút "Authorize" ở góc trên bên phải
   - Nhập: `Bearer <your-token>`
   - Click "Authorize"
   - Click "Close"

3. Test các endpoint được bảo vệ:
   - Bây giờ bạn có thể test các endpoint cần authentication

### 3. Ví dụ Request/Response

**Login Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Login Response:**
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "authenticated": true
  }
}
```

## JWT Configuration

### Token Properties

- **Access Token Duration**: 1 giờ (3600 seconds)
- **Refresh Token Duration**: 24 giờ (86400 seconds)
- **Algorithm**: HS512
- **Issuer**: manh

### Token Claims

```json
{
  "sub": "user@example.com",
  "iss": "manh",
  "iat": 1234567890,
  "exp": 1234571490,
  "jti": "uuid-here",
  "scope": "ROLE_USER PERMISSION_1 PERMISSION_2"
}
```

## Security Configuration

### Public Endpoints (không cần token)

- `POST /users/register`
- `GET /users/confirm`
- `POST /auth/token`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /auth/introspect`
- `POST /users/forgot-password`

### Protected Endpoints (cần token)

- `GET /users/myInfo`
- `PUT /users/my-info`
- `PUT /users/change-password`
- Tất cả các endpoint khác

### Admin Only Endpoints

- `POST /auth/infinite-token`
- `GET /users` (list all users)
- `POST /users/{id}/lock`
- `POST /users/{id}/unlock`

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    full_name VARCHAR(255),
    number_phone VARCHAR(20),
    dob DATE,
    avatar VARCHAR(500),
    gender BOOLEAN,
    email_verified BOOLEAN DEFAULT FALSE,
    auth_provider VARCHAR(50),
    locked BOOLEAN DEFAULT FALSE,
    locked_at TIMESTAMP,
    locked_by VARCHAR(36),
    lock_reason TEXT,
    joined_date TIMESTAMP,
    balance DECIMAL(15,2) DEFAULT 0.00,
    revenue DECIMAL(15,2) DEFAULT 0.00,
    bank_name VARCHAR(100),
    bank_account_holder_name VARCHAR(100),
    bank_account_number VARCHAR(20),
    FOREIGN KEY (locked_by) REFERENCES users(id)
);
```

### Roles Table

```sql
CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT
);
```

### Permissions Table

```sql
CREATE TABLE permission (
    name VARCHAR(255) PRIMARY KEY,
    description TEXT
);
```

### User_Roles Table

```sql
CREATE TABLE user_roles (
    user_id VARCHAR(36),
    role_id VARCHAR(36),
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

### Role_Permissions Table

```sql
CREATE TABLE role_permissions (
    role_id VARCHAR(36),
    permission_id VARCHAR(255),
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permission(name)
);
```

### Invalidated_Token Table

```sql
CREATE TABLE invalidated_token (
    id VARCHAR(255) PRIMARY KEY,
    expiry_time TIMESTAMP
);
```

## Resilience4j Configuration

### Circuit Breaker

- **Sliding Window Size**: 10 requests
- **Failure Rate Threshold**: 50%
- **Wait Duration in Open State**: 5 seconds

### Rate Limiter

- **Limit for Period**: 10 requests
- **Limit Refresh Period**: 1 second

### Retry

- **Max Attempts**: 3
- **Wait Duration**: 1 second
- **Exponential Backoff**: Enabled (multiplier: 2)

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Thành công |
| 1001 | Uncategorized Exception | Lỗi không xác định |
| 1002 | Invalid Key | Key không hợp lệ |
| 1003 | User Existed | User đã tồn tại |
| 1004 | Username Invalid | Username không hợp lệ |
| 1005 | Invalid Password | Password không hợp lệ |
| 1006 | User Not Existed | User không tồn tại |
| 1007 | Unauthenticated | Chưa xác thực |
| 1008 | Unauthorized | Không có quyền |
| 1009 | Invalid DOB | Ngày sinh không hợp lệ |
| 1010 | User Locked | Tài khoản bị khóa |

## Development

### Thêm Swagger Annotations

```java
@Tag(name = "User Management", description = "API quản lý người dùng")
@RestController
@RequestMapping("/users")
public class UserController {
    
    @Operation(
        summary = "Lấy thông tin user",
        description = "Lấy thông tin chi tiết của user hiện tại"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
        @ApiResponse(responseCode = "403", description = "Không có quyền")
    })
    @GetMapping("/myInfo")
    public ApiResponse<UserResponse> getMyInfo() {
        // ...
    }
}
```

### Thêm Entity mới

1. Tạo Entity class trong `entity/`
2. Tạo Repository interface trong `repository/`
3. Tạo DTO Request/Response trong `dto/`
4. Tạo Mapper interface trong `mapper/`
5. Tạo Service class trong `service/`
6. Tạo Controller class trong `controller/`

## Testing

### Test với Swagger UI

1. Truy cập http://localhost:8080/swagger-ui.html
2. Test các endpoint trực tiếp từ UI
3. Xem request/response examples

### Test với Postman

Import Swagger JSON từ: http://localhost:8080/api-docs

### Test với cURL

```bash
# Login
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Get My Info
curl -X GET http://localhost:8080/users/myInfo \
  -H "Authorization: Bearer <your-token>"
```

## Troubleshooting

### Lỗi MapStruct

```bash
mvn clean compile
```

### Lỗi Lombok

- Cài Lombok plugin cho IDE
- Enable annotation processing trong IDE settings

### Lỗi JWT

- Kiểm tra `jwt.signerKey` trong application.yaml (phải >= 32 ký tự)
- Kiểm tra token có hết hạn chưa

### Lỗi Database

- Kiểm tra MySQL đã chạy
- Kiểm tra username/password trong application.yaml
- Tạo database trước khi chạy app

### Lỗi Port đã được sử dụng

```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8080
kill -9 <PID>
```

## License

Apache 2.0

## Contact

- Email: support@wvideos.com
- Website: https://wvideos.com
