# SQL Scripts

Các SQL scripts để setup và quản lý database.

## 📄 Available Scripts

### 1. create-db.sql
Tạo database và tables từ đầu với cấu trúc hoàn chỉnh.

**Sử dụng:**
```bash
mysql -u root -p < create-db.sql
```

**Bao gồm:**
- Tạo database `db_wvideos`
- Tạo tất cả tables (users, roles, permissions, etc.)
- Insert data mẫu (roles, permissions, admin user)

### 2. init-database.sql
Script khởi tạo database chi tiết hơn với nhiều options.

**Sử dụng:**
```bash
mysql -u root -p < init-database.sql
```

## 🔑 Default Data

Sau khi chạy scripts, bạn sẽ có:

**Admin User:**
- Username: `admin`
- Email: `admin@wvideos.com`
- Password: `admin123`

**Roles:**
- ADMIN - Quản trị viên
- USER - Người dùng thông thường
- GUEST - Khách

**Permissions:**
- CREATE_USER
- UPDATE_USER
- DELETE_USER
- VIEW_USER
