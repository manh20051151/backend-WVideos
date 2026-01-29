# Hướng dẫn Reset Database Manual

## Vấn đề
Database hiện tại có schema cũ với `id INT`, nhưng Entity định nghĩa `id VARCHAR(36)` (UUID).

## Giải pháp: Reset Database Manual

### Bước 1: Mở MySQL Command Line hoặc MySQL Workbench

### Bước 2: Chạy các lệnh sau

```sql
-- Drop database cũ
DROP DATABASE IF EXISTS db_wvideos;

-- Tạo database mới
CREATE DATABASE db_wvideos 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Sử dụng database
USE db_wvideos;
```

### Bước 3: Chạy init script

Có 2 cách:

**Cách 1: Copy nội dung file `scripts/sql/init-database.sql` và paste vào MySQL**

**Cách 2: Sử dụng command line**

```bash
# Tìm đường dẫn MySQL
# Thường là: C:\xampp\mysql\bin\mysql.exe
# Hoặc: C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe

# Chạy lệnh (thay đổi đường dẫn phù hợp)
C:\xampp\mysql\bin\mysql.exe -u root -psapassword db_wvideos < scripts/sql/init-database.sql
```

### Bước 4: Verify

```sql
USE db_wvideos;

-- Kiểm tra tables
SHOW TABLES;

-- Kiểm tra cấu trúc table users
DESC users;

-- Kiểm tra id column phải là VARCHAR(36)
-- Kết quả mong đợi:
-- Field: id
-- Type: varchar(36)
-- Null: NO
-- Key: PRI
```

### Bước 5: Chạy lại application

```bash
.\mvnw.cmd spring-boot:run
```

## Lưu ý

- Password MySQL: `sapassword`
- Username: `root`
- Database: `db_wvideos`
- Port: 3306

## Nếu gặp lỗi "auth_gssapi_client.dll"

Thêm option `--skip-ssl` khi chạy MySQL command:

```bash
C:\xampp\mysql\bin\mysql.exe -u root -psapassword --skip-ssl db_wvideos < scripts/sql/init-database.sql
```
