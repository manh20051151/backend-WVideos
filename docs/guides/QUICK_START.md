# Quick Start Guide - WVideos Backend

Hướng dẫn nhanh để chạy WVideos Backend API với JWT Authentication và Swagger.

## Bước 1: Chuẩn bị

### Yêu cầu hệ thống
- ✅ Java 17 hoặc cao hơn
- ✅ Maven 3.6+
- ✅ MySQL 8.0+
- ✅ IDE (IntelliJ IDEA, Eclipse, hoặc VS Code)

### Kiểm tra Java version
```bash
java -version
# Phải hiển thị: java version "17" hoặc cao hơn
```

### Kiểm tra Maven
```bash
mvn -version
```

## Bước 2: Setup Database

### 2.1. Khởi động MySQL
Đảm bảo MySQL đang chạy trên port 3306.

### 2.2. Chạy script khởi tạo
```bash
# Đăng nhập MySQL
mysql -u root -p

# Chạy script
source init-database.sql

# Hoặc
mysql -u root -p < init-database.sql
```

Script sẽ tự động:
- Tạo database `db_wvideos`
- Tạo các bảng (users, roles, permissions, etc.)
- Insert dữ liệu mẫu (admin user, test user, roles, permissions)

### 2.3. Verify database
```sql
USE db_wvideos;
SHOW TABLES;
SELECT * FROM users;
```

## Bước 3: Cấu hình Application

### 3.1. Cập nhật application.yaml

Mở file `src/main/resources/application.yaml` và cập nhật:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_wvideos
    username: root
    password: YOUR_MYSQL_PASSWORD  # ⚠️ Thay đổi password của bạn
  
  mail:
    username: your-email@gmail.com  # ⚠️ Thay đổi email của bạn
    password: your-app-password     # ⚠️ Thay đổi app password

jwt:
  signerKey: at-least-32-characters-secret-key-for-jwt-signing-wvideos-2024
  # ⚠️ Nên thay đổi key này trong production
```

### 3.2. Tạo Gmail App Password (nếu dùng email)

1. Truy cập: https://myaccount.google.com/apppasswords
2. Tạo app password mới
3. Copy password và paste vào `spring.mail.password`

## Bước 4: Build và Run

### 4.1. Build project
```bash
cd e:\project\WVideos\backendWVideos
mvn clean install
```

Nếu thành công, bạn sẽ thấy:
```
[INFO] BUILD SUCCESS
```

### 4.2. Run application
```bash
mvn spring-boot:run
```

Hoặc chạy từ IDE:
- IntelliJ IDEA: Click nút Run ▶️
- Eclipse: Right click → Run As → Spring Boot App
- VS Code: F5 hoặc Run → Start Debugging

### 4.3. Kiểm tra application đã chạy

Mở trình duyệt và truy cập:
- ✅ http://localhost:8080 → Nếu thấy Whitelabel Error Page là OK
- ✅ http://localhost:8080/swagger-ui.html → Swagger UI

## Bước 5: Test API với Swagger

### 5.1. Truy cập Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 5.2. Login để lấy JWT Token

1. Mở endpoint: `POST /auth/token`
2. Click "Try it out"
3. Nhập thông tin:
   ```json
   {
     "email": "admin@wvideos.com",
     "password": "admin123"
   }
   ```
4. Click "Execute"
5. Copy token từ response:
   ```json
   {
     "code": 1000,
     "result": {
       "token": "eyJhbGciOiJIUzUxMiJ9...",  ← Copy cái này
       "authenticated": true
     }
   }
   ```

### 5.3. Authorize với Token

1. Click nút **"Authorize"** ở góc trên bên phải
2. Nhập: `Bearer <paste-token-ở-đây>`
   ```
   Bearer eyJhbGciOiJIUzUxMiJ9...
   ```
3. Click "Authorize"
4. Click "Close"

### 5.4. Test các endpoint

Bây giờ bạn có thể test các endpoint:

**Lấy thông tin user hiện tại:**
- Endpoint: `GET /users/myInfo`
- Click "Try it out" → "Execute"
- Xem response

**Lấy danh sách users (Admin only):**
- Endpoint: `GET /users`
- Click "Try it out" → "Execute"

**Đổi mật khẩu:**
- Endpoint: `PUT /users/change-password`
- Body:
  ```json
  {
    "passwordOld": "admin123",
    "password": "newpassword123"
  }
  ```

## Bước 6: Test với Postman (Optional)

### 6.1. Import Swagger JSON
1. Mở Postman
2. Import → Link
3. Nhập: `http://localhost:8080/api-docs`
4. Import

### 6.2. Test Login
```
POST http://localhost:8080/auth/token
Content-Type: application/json

{
  "email": "admin@wvideos.com",
  "password": "admin123"
}
```

### 6.3. Test với Token
```
GET http://localhost:8080/users/myInfo
Authorization: Bearer <your-token>
```

## Bước 7: Test với cURL

### Login
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@wvideos.com\",\"password\":\"admin123\"}"
```

### Get My Info
```bash
curl -X GET http://localhost:8080/users/myInfo \
  -H "Authorization: Bearer <your-token>"
```

## Tài khoản mặc định

| Email | Password | Role | Description |
|-------|----------|------|-------------|
| admin@wvideos.com | admin123 | ADMIN | Quản trị viên - có toàn quyền |
| user@wvideos.com | user123 | USER | Người dùng thông thường |

## Các endpoint quan trọng

### Authentication
- `POST /auth/token` - Đăng nhập
- `POST /auth/refresh` - Refresh token
- `POST /auth/logout` - Đăng xuất
- `POST /auth/introspect` - Kiểm tra token

### User Management
- `POST /users/register` - Đăng ký
- `GET /users/myInfo` - Thông tin user hiện tại
- `PUT /users/my-info` - Cập nhật thông tin
- `PUT /users/change-password` - Đổi mật khẩu
- `GET /users` - Danh sách users (Admin)

## Troubleshooting

### Lỗi: Port 8080 already in use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8080
kill -9 <PID>
```

### Lỗi: Cannot connect to database
- Kiểm tra MySQL đã chạy chưa
- Kiểm tra username/password trong application.yaml
- Kiểm tra database `db_wvideos` đã được tạo chưa

### Lỗi: JWT token invalid
- Token có thể đã hết hạn (1 giờ)
- Login lại để lấy token mới
- Kiểm tra `jwt.signerKey` trong application.yaml

### Lỗi: MapStruct/Lombok
```bash
# Clean và rebuild
mvn clean install

# Nếu dùng IDE, enable annotation processing:
# IntelliJ: Settings → Build → Compiler → Annotation Processors → Enable
# Eclipse: Project Properties → Java Compiler → Annotation Processing → Enable
```

### Lỗi: Swagger UI không hiển thị
- Kiểm tra application đã chạy chưa
- Truy cập: http://localhost:8080/swagger-ui.html (có /swagger-ui.html)
- Xem logs để kiểm tra lỗi

## Logs

Xem logs trong console khi chạy application:
```
2024-01-29 10:00:00 - Started BackendWVideosApplication in 5.123 seconds
2024-01-29 10:00:00 - Swagger UI: http://localhost:8080/swagger-ui.html
```

## Next Steps

Sau khi setup thành công:

1. ✅ Đọc file `README.md` để hiểu chi tiết về project
2. ✅ Đọc file `COPY_USER_API_GUIDE.md` để biết cách thêm features mới
3. ✅ Thêm entities mới cho WVideos (Video, Channel, Comment, etc.)
4. ✅ Tích hợp với Frontend
5. ✅ Deploy lên server

## Liên hệ

Nếu gặp vấn đề, liên hệ:
- Email: support@wvideos.com
- GitHub Issues: [link-to-repo]

---

**Chúc bạn code vui vẻ! 🚀**
