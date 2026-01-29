# Hướng Dẫn Test API với Swagger

## 🚀 Truy cập Swagger UI

Application đã chạy thành công tại: **http://localhost:8080**

Truy cập Swagger UI tại: **http://localhost:8080/swagger-ui.html**

## 📋 Các API Endpoints Chính

### 1. Authentication APIs (`/auth`)

#### Login
- **POST** `/auth/token`
- Body:
```json
{
  "username": "admin",
  "password": "admin123"
}
```
- Response: Nhận được `access_token` và `refresh_token`

#### Introspect Token
- **POST** `/auth/introspect`
- Body:
```json
{
  "token": "your_access_token_here"
}
```

#### Refresh Token
- **POST** `/auth/refresh`
- Body:
```json
{
  "token": "your_refresh_token_here"
}
```

#### Logout
- **POST** `/auth/logout`
- Body:
```json
{
  "token": "your_access_token_here"
}
```

### 2. User APIs (`/users`)

#### Đăng ký User mới
- **POST** `/users/register`
- Body:
```json
{
  "username": "testuser",
  "password": "Test@123",
  "email": "test@example.com",
  "fullName": "Test User",
  "numberPhone": "0123456789"
}
```

#### Xác nhận đăng ký
- **GET** `/users/confirm?token={registration_token}`
- Token sẽ được gửi qua email

#### Lấy thông tin user hiện tại
- **GET** `/users/myInfo`
- Headers: `Authorization: Bearer {access_token}`

#### Lấy danh sách tất cả users
- **GET** `/users`
- Headers: `Authorization: Bearer {access_token}`

#### Lấy thông tin user theo ID
- **GET** `/users/{userId}`
- Headers: `Authorization: Bearer {access_token}`

#### Cập nhật thông tin cá nhân
- **PUT** `/users/my-info`
- Headers: `Authorization: Bearer {access_token}`
- Body:
```json
{
  "fullName": "Updated Name",
  "numberPhone": "0987654321",
  "email": "newemail@example.com",
  "gender": true,
  "dob": "1990-01-01"
}
```

#### Đổi mật khẩu
- **PUT** `/users/change-password`
- Headers: `Authorization: Bearer {access_token}`
- Body:
```json
{
  "passwordOld": "old_password",
  "passwordNew": "new_password"
}
```

#### Cập nhật thông tin ngân hàng
- **PUT** `/users/my-bank-info`
- Headers: `Authorization: Bearer {access_token}`
- Body:
```json
{
  "bankName": "Vietcombank",
  "bankAccountHolderName": "NGUYEN VAN A",
  "bankAccountNumber": "1234567890"
}
```

#### Quên mật khẩu
- **POST** `/users/forgot-password`
- Body:
```json
{
  "email": "user@example.com"
}
```

#### Khóa tài khoản (Admin)
- **POST** `/users/{userId}/lock?lockedById={adminId}&reason={reason}`
- Headers: `Authorization: Bearer {admin_access_token}`

#### Mở khóa tài khoản (Admin)
- **POST** `/users/{userId}/unlock`
- Headers: `Authorization: Bearer {admin_access_token}`

#### Lấy danh sách tài khoản bị khóa
- **GET** `/users/locked?page=0&size=10`
- Headers: `Authorization: Bearer {admin_access_token}`

## 🔐 Cách sử dụng JWT Token trong Swagger

1. **Login** để lấy access_token
2. Click vào nút **"Authorize"** ở góc trên bên phải Swagger UI
3. Nhập: `Bearer {your_access_token}`
4. Click **"Authorize"**
5. Bây giờ bạn có thể test các API cần authentication

## 👤 Tài khoản Admin mặc định

- **Username**: `admin`
- **Email**: `admin@wvideos.com`
- **Password**: `admin123`

## 📊 Database Schema

Database: `db_wvideos`

Tables:
- `users` - Thông tin người dùng
- `roles` - Vai trò (ADMIN, USER, GUEST)
- `permissions` - Quyền hạn
- `user_roles` - Mapping user-role
- `role_permissions` - Mapping role-permission
- `invalidated_token` - Token đã logout
- `pending_registration` - Đăng ký chờ xác nhận

## 🎯 Test Flow Cơ Bản

### Flow 1: Đăng ký và Login
1. POST `/users/register` - Đăng ký user mới
2. GET `/users/confirm?token=xxx` - Xác nhận email (check email để lấy token)
3. POST `/auth/token` - Login với username/password
4. GET `/users/myInfo` - Lấy thông tin user (dùng access_token)

### Flow 2: Quản lý thông tin cá nhân
1. POST `/auth/token` - Login
2. GET `/users/myInfo` - Xem thông tin hiện tại
3. PUT `/users/my-info` - Cập nhật thông tin
4. PUT `/users/change-password` - Đổi mật khẩu
5. PUT `/users/my-bank-info` - Cập nhật thông tin ngân hàng

### Flow 3: Admin quản lý users
1. POST `/auth/token` - Login với admin account
2. GET `/users` - Xem danh sách users
3. POST `/users/{userId}/lock` - Khóa user
4. GET `/users/locked` - Xem danh sách user bị khóa
5. POST `/users/{userId}/unlock` - Mở khóa user

## 🔧 Troubleshooting

### Lỗi 401 Unauthorized
- Kiểm tra token có hợp lệ không
- Token có thể đã hết hạn, cần refresh hoặc login lại
- Kiểm tra đã click "Authorize" trong Swagger chưa

### Lỗi 403 Forbidden
- User không có quyền truy cập endpoint này
- Cần login với tài khoản có role phù hợp (VD: ADMIN)

### Lỗi 404 Not Found
- Kiểm tra URL endpoint có đúng không
- Kiểm tra userId có tồn tại trong database không

## 📝 Notes

- Access token có thời gian sống ngắn (1 giờ)
- Refresh token có thời gian sống dài hơn (30 ngày)
- Email confirmation token hết hạn sau 30 phút
- Password phải có ít nhất 8 ký tự
- Tất cả API responses đều wrap trong `ApiResponse<T>` format

## 🎉 Kết luận

API đã sẵn sàng để test! Truy cập Swagger UI và bắt đầu thử nghiệm các endpoints.

Happy Testing! 🚀
