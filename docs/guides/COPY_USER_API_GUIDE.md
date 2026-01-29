# Hướng dẫn Copy User API từ DocPro Backend

## Tổng quan
Document này hướng dẫn copy toàn bộ User API và các class liên quan từ `e:\projectDoc\source\backend` sang `e:\project\WVideos\backendWVideos`, kèm tích hợp Swagger.

## Cấu trúc Package
- Source: `iuh.fit.backend`
- Target: `com.example.backendWVideos`

## Danh sách file cần copy

### 1. Entity (7 files)
```
backend/src/main/java/iuh/fit/backend/entity/
├── User.java
├── Role.java
├── Permission.java
├── InvalidatedToken.java
└── Document.java (nếu cần - có reference trong User)
```

### 2. Repository (4 files)
```
backend/src/main/java/iuh/fit/backend/repository/
├── UserRepository.java
├── RoleRepository.java
├── PermissionRepository.java
└── InvalidatedTokenRepository.java
```

### 3. DTO Request (10+ files)
```
backend/src/main/java/iuh/fit/backend/dto/request/
├── AuthenticationRequest.java
├── IntrospectRequest.java
├── RefreshRequest.java
├── LogoutRequest.java
├── InfiniteTokenRequest.java
├── UserCreateRequest.java
├── UserUpdateRequest.java
├── UserUpdateByUserRequest.java
└── ChangePasswordRequest.java
```

### 4. DTO Response (10+ files)
```
backend/src/main/java/iuh/fit/backend/dto/response/
├── AuthenticationResponse.java
├── IntrospectResponse.java
├── UserResponse.java
├── RoleResponse.java
├── PermissionResponse.java
└── ApiResponse.java (wrapper chung)
```

### 5. Service (3 files)
```
backend/src/main/java/iuh/fit/backend/service/
├── AuthenticationService.java
├── UserService.java
└── RoleService.java
```

### 6. Controller (2 files)
```
backend/src/main/java/iuh/fit/backend/controller/
├── AuthenticationController.java
├── UserController.java
└── ApiResponse.java (nếu ở đây)
```

### 7. Config (8 files)
```
backend/src/main/java/iuh/fit/backend/config/
├── SecurityConfig.java
├── CustomJwtDecoder.java
├── JwtAuthenticationEntryPoint.java
├── ApplicationInitConfig.java
├── CustomOAuth2AuthorizationRequestResolver.java
├── OAuth2LoginSuccessHandler.java
└── UserArgumentResolver.java
```

### 8. Security (2 files)
```
backend/src/main/java/iuh/fit/backend/security/
├── CurrentUser.java (annotation)
└── SecurityUtils.java (nếu có)
```

### 9. Exception (3 files)
```
backend/src/main/java/iuh/fit/backend/exception/
├── AppException.java
├── ErrorCode.java
└── GlobalExceptionHandler.java
```

### 10. Enums (2 files)
```
backend/src/main/java/iuh/fit/backend/enums/
├── AuthProvider.java
└── Role.java (nếu có enum)
```

### 11. Mapper (3 files)
```
backend/src/main/java/iuh/fit/backend/mapper/
├── UserMapper.java
├── RoleMapper.java
└── PermissionMapper.java
```

### 12. Validator (nếu có)
```
backend/src/main/java/iuh/fit/backend/validator/
├── DobValidator.java
└── EmailValidator.java
```

## Các bước thực hiện

### Bước 1: Cập nhật pom.xml
✅ Đã hoàn thành - Thêm dependencies:
- Spring Security OAuth2
- JWT (Nimbus JOSE)
- Lombok & MapStruct
- Swagger/OpenAPI (springdoc-openapi-starter-webmvc-ui)
- Resilience4j
- Email support

### Bước 2: Cập nhật application.yaml
Cần thêm cấu hình:
```yaml
spring:
  application:
    name: backendWVideos
  datasource:
    url: jdbc:mysql://localhost:3306/db_wvideos
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

jwt:
  signerKey: your-secret-key-at-least-32-characters-long
  valid-duration: 3600 # 1 hour
  refreshable-duration: 86400 # 24 hours

app:
  frontend-url: http://localhost:5173

# Swagger/OpenAPI
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha
    operations-sorter: alpha
  show-actuator: false

# Resilience4j
resilience4j:
  circuitbreaker:
    instances:
      authentication:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
  ratelimiter:
    instances:
      authentication:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 0s
  retry:
    instances:
      authentication:
        maxAttempts: 3
        waitDuration: 1s
```

### Bước 3: Tạo cấu trúc thư mục
```
src/main/java/com/example/backendWVideos/
├── config/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── entity/
├── enums/
├── exception/
├── mapper/
├── repository/
├── security/
├── service/
└── validator/
```

### Bước 4: Copy và sửa package name
Tất cả các file cần thay đổi:
- `package iuh.fit.backend` → `package com.example.backendWVideos`
- `import iuh.fit.backend` → `import com.example.backendWVideos`

### Bước 5: Tích hợp Swagger
Tạo file `SwaggerConfig.java`:
```java
package com.example.backendWVideos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WVideos Backend API")
                        .version("1.0.0")
                        .description("API documentation cho WVideos Backend với JWT Authentication")
                        .contact(new Contact()
                                .name("Your Name")
                                .email("your-email@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url(frontendUrl).description("Frontend URL")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập JWT token vào đây. Token có thể lấy từ endpoint /auth/token")));
    }
}
```

### Bước 6: Thêm Swagger annotations vào Controllers
Ví dụ cho `AuthenticationController`:
```java
@Tag(name = "Authentication", description = "API xác thực người dùng - Login, Logout, Refresh Token")
@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    
    @Operation(
        summary = "Đăng nhập",
        description = "Xác thực người dùng bằng email và password, trả về JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
        @ApiResponse(responseCode = "401", description = "Email hoặc password không đúng"),
        @ApiResponse(responseCode = "403", description = "Tài khoản bị khóa")
    })
    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> login(
            @RequestBody @Valid AuthenticationRequest request) {
        // ...
    }
}
```

### Bước 7: Tạo database
```sql
CREATE DATABASE IF NOT EXISTS db_wvideos 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### Bước 8: Build và chạy
```bash
cd e:\project\WVideos\backendWVideos
mvn clean install
mvn spring-boot:run
```

### Bước 9: Truy cập Swagger UI
Sau khi chạy ứng dụng, truy cập:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs JSON: http://localhost:8080/api-docs

## Script PowerShell để copy tự động

Xem file `copy-user-api.ps1` để copy tự động tất cả các file.

## Lưu ý quan trọng

1. **Document entity**: User có reference đến Document, cần quyết định:
   - Copy cả Document entity
   - Hoặc comment out các field liên quan đến Document trong User

2. **OAuth2**: Nếu không dùng Google/Facebook login, có thể bỏ:
   - OAuth2LoginSuccessHandler
   - CustomOAuth2AuthorizationRequestResolver
   - Dependencies oauth2-client

3. **Email**: Cần cấu hình SMTP để gửi email xác thực

4. **Cloudinary**: Nếu User có avatar upload, cần cấu hình Cloudinary

5. **Resilience4j**: Circuit breaker, rate limiter - có thể bỏ nếu không cần

## Testing

Sau khi copy xong, test các endpoint:

1. **Register**: POST /users/register
2. **Login**: POST /auth/token
3. **Get My Info**: GET /users/myInfo (cần token)
4. **Refresh Token**: POST /auth/refresh
5. **Logout**: POST /auth/logout

## Swagger Testing

1. Mở Swagger UI: http://localhost:8080/swagger-ui.html
2. Click "Authorize" button
3. Nhập token: `Bearer <your-jwt-token>`
4. Test các endpoint trực tiếp từ Swagger UI

## Troubleshooting

### Lỗi MapStruct
```bash
mvn clean compile
```

### Lỗi Lombok
- Cài Lombok plugin cho IDE
- Enable annotation processing

### Lỗi JWT
- Kiểm tra signerKey trong application.yaml (phải >= 32 ký tự)

### Lỗi Database
- Kiểm tra MySQL đã chạy
- Kiểm tra username/password
- Tạo database trước

## Kết luận

Sau khi hoàn thành, bạn sẽ có:
- ✅ User API hoàn chỉnh với JWT Authentication
- ✅ Swagger UI để test và document API
- ✅ Security config với Spring Security
- ✅ Exception handling
- ✅ DTO mapping với MapStruct
- ✅ Email support
- ✅ OAuth2 support (optional)
