# Hướng dẫn Enable Google OAuth2 Login

## Tổng quan
Backend đã có sẵn OAuth2 Google setup nhưng đang bị disable. Tài liệu này hướng dẫn cách enable và cấu hình.

## Bước 1: Tạo Google OAuth2 Credentials

### 1.1. Truy cập Google Cloud Console
1. Mở https://console.cloud.google.com/
2. Tạo project mới hoặc chọn project hiện có
3. Vào **APIs & Services** > **Credentials**

### 1.2. Tạo OAuth 2.0 Client ID
1. Click **Create Credentials** > **OAuth client ID**
2. Chọn **Application type**: Web application
3. Điền thông tin:
   - **Name**: WVideos OAuth2
   - **Authorized JavaScript origins**:
     ```
     http://localhost:3000
     http://localhost:8080
     ```
   - **Authorized redirect URIs**:
     ```
     http://localhost:8080/api/oauth2/callback/google
     http://localhost:8080/api/login/oauth2/code/google
     https://localhost:8080/api/oauth2/callback/google
     ```
     
     **Lưu ý quan trọng:** 
     - Phải thêm cả HTTP và HTTPS redirect URIs
     - Format chính xác: `http://localhost:8080/api/oauth2/callback/google`
     - Không có trailing slash `/` ở cuối
4. Click **Create**
5. Copy **Client ID** và **Client Secret**

## Bước 2: Cấu hình Backend

### 2.1. Thêm OAuth2 config vào `application.yaml`

Thêm section sau vào file `src/main/resources/application.yaml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
            scope:
              - email
              - profile
            redirect-uri: '{baseUrl}/oauth2/callback/{registrationId}'
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
            user-name-attribute: sub
```

**Lưu ý:** Thay `YOUR_GOOGLE_CLIENT_ID` và `YOUR_GOOGLE_CLIENT_SECRET` bằng giá trị thực tế.

### 2.2. Enable OAuth2 trong SecurityConfig

Mở file `src/main/java/com/example/backendWVideos/config/SecurityConfig.java` và uncomment các dòng OAuth2:

**Tìm đoạn code:**
```java
// Disable OAuth2 Login for now - can be enabled later
// .oauth2Login(oauth2 -> oauth2
//     .successHandler(oAuth2LoginSuccessHandler)
//     .failureUrl("/login?error=true")
//     .authorizationEndpoint(authorization -> authorization
//         .baseUri("/oauth2/authorization")
//         .authorizationRequestResolver(authorizationRequestResolver()))
//     .redirectionEndpoint(redirection -> redirection
//         .baseUri("/oauth2/callback/*"))
// )
```

**Thay bằng:**
```java
.oauth2Login(oauth2 -> oauth2
    .successHandler(oAuth2LoginSuccessHandler)
    .failureUrl("/login?error=true")
    .authorizationEndpoint(authorization -> authorization
        .baseUri("/oauth2/authorization")
        .authorizationRequestResolver(authorizationRequestResolver()))
    .redirectionEndpoint(redirection -> redirection
        .baseUri("/oauth2/callback/*"))
)
```

**Và uncomment OAuth2AuthorizationRequestResolver bean:**
```java
@Bean
public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository) {
    DefaultOAuth2AuthorizationRequestResolver defaultResolver = 
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            "/oauth2/authorization");
    return new CustomOAuth2AuthorizationRequestResolver(defaultResolver);
}
```

### 2.3. Thêm dependency (nếu chưa có)

Kiểm tra `pom.xml` có dependency sau:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

## Bước 3: Test OAuth2 Login

### 3.1. Restart Backend
```bash
cd WVideos/backendWVideos
mvn spring-boot:run
```

### 3.2. Test từ Frontend
1. Mở http://localhost:3000
2. Click "Đăng nhập"
3. Click "Đăng nhập với Google"
4. Chọn tài khoản Google
5. Cho phép quyền truy cập
6. Sẽ redirect về trang chủ với token

### 3.3. Test trực tiếp Backend
Mở trình duyệt và truy cập:
```
http://localhost:8080/api/oauth2/authorization/google
```

Sẽ redirect đến Google login page.

## Luồng hoạt động

```
1. User click "Đăng nhập với Google"
   ↓
2. Frontend redirect: http://localhost:8080/api/oauth2/authorization/google
   ↓
3. Backend redirect đến Google OAuth2
   ↓
4. User đăng nhập Google và cho phép quyền
   ↓
5. Google redirect về: http://localhost:8080/api/oauth2/callback/google?code=...
   ↓
6. Backend xử lý callback:
   - Lấy user info từ Google
   - Tạo/cập nhật user trong database
   - Generate JWT token
   ↓
7. Backend redirect về Frontend: http://localhost:3000/oauth2/redirect?token=...
   ↓
8. Frontend lưu token vào localStorage
   ↓
9. Redirect về trang chủ
```

## Xử lý lỗi

### Lỗi: redirect_uri_mismatch
**Nguyên nhân:** Redirect URI không khớp với config trong Google Console

**Giải pháp:**
1. Kiểm tra lại Authorized redirect URIs trong Google Console
2. Đảm bảo có đúng format: `http://localhost:8080/api/oauth2/callback/google`

### Lỗi: User locked
**Nguyên nhân:** Tài khoản bị khóa

**Giải pháp:**
- Backend sẽ redirect về: `http://localhost:3000/oauth2/redirect?error=user_locked`
- Frontend hiển thị thông báo: "Tài khoản của bạn đã bị khóa"

### Lỗi: Authentication failed
**Nguyên nhân:** Lỗi trong quá trình xác thực

**Giải pháp:**
1. Kiểm tra logs backend
2. Kiểm tra Client ID và Secret
3. Kiểm tra database connection

## Production Setup

### Cập nhật Authorized URIs
Thêm production URLs vào Google Console:
```
https://yourdomain.com
https://api.yourdomain.com/oauth2/callback/google
```

### Cập nhật application.yaml
```yaml
app:
  frontend-url: https://yourdomain.com
```

### Environment Variables
Nên dùng environment variables thay vì hardcode:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
```

## Security Notes

1. **Client Secret**: Không commit vào git, dùng environment variables
2. **HTTPS**: Production phải dùng HTTPS
3. **CORS**: Đảm bảo frontend URL được config trong CORS
4. **Token Expiry**: JWT token có thời gian hết hạn (mặc định 1 giờ)

## Troubleshooting

### Check OAuth2 endpoints
```bash
# List all OAuth2 providers
curl http://localhost:8080/api/oauth2/authorization/google

# Check if OAuth2 is enabled
# Xem logs khi start backend, tìm dòng:
# "OAuth2 Login enabled with providers: [google]"
```

### Debug mode
Thêm vào `application.yaml`:
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
```

## Tài liệu tham khảo

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Spring Boot OAuth2 Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/)
