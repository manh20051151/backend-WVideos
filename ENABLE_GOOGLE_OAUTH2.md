# Enable Google OAuth2 - Quick Start

## Bước 1: Thêm config vào application.yaml

Thêm section sau vào file `src/main/resources/application.yaml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID_HERE
            client-secret: YOUR_GOOGLE_CLIENT_SECRET_HERE
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

## Bước 2: Lấy Google Client ID và Secret

1. Truy cập: https://console.cloud.google.com/
2. Tạo project mới hoặc chọn project hiện có
3. Vào **APIs & Services** > **Credentials**
4. Click **Create Credentials** > **OAuth client ID**
5. Chọn **Web application**
6. Thêm **Authorized redirect URIs**:
   ```
   http://localhost:8080/api/oauth2/callback/google
   http://localhost:8080/api/login/oauth2/code/google
   ```
7. Copy Client ID và Client Secret

## Bước 3: Cập nhật application.yaml

Thay `YOUR_GOOGLE_CLIENT_ID_HERE` và `YOUR_GOOGLE_CLIENT_SECRET_HERE` bằng giá trị thực tế.

## Bước 4: Restart Backend

```bash
# Stop backend nếu đang chạy (Ctrl+C)
# Start lại
mvn spring-boot:run
```

## Test

1. Mở trình duyệt: http://localhost:8080/api/oauth2/authorization/google
2. Sẽ redirect đến Google login
3. Sau khi đăng nhập, sẽ redirect về frontend với token

## Lưu ý

- SecurityConfig đã được cập nhật để enable OAuth2
- OAuth2 endpoints đã được thêm vào PUBLIC_ENDPOINTS
- Không cần restart nếu chỉ thay đổi Client ID/Secret trong application.yaml

## Troubleshooting

### Lỗi: redirect_uri_mismatch
Kiểm tra lại Authorized redirect URIs trong Google Console phải khớp với:
```
http://localhost:8080/api/oauth2/callback/google
```

### Lỗi: Không xác thực
- Đảm bảo đã thêm OAuth2 config vào application.yaml
- Restart backend sau khi thêm config
