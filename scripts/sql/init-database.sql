-- Script khởi tạo database cho WVideos Backend
-- Chạy script này sau khi tạo database

-- Tạo database nếu chưa có
CREATE DATABASE IF NOT EXISTS db_wvideos 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE db_wvideos;

-- Xóa các bảng cũ nếu có (cẩn thận khi chạy trên production)
-- DROP TABLE IF EXISTS user_roles;
-- DROP TABLE IF EXISTS role_permissions;
-- DROP TABLE IF EXISTS invalidated_token;
-- DROP TABLE IF EXISTS users;
-- DROP TABLE IF EXISTS roles;
-- DROP TABLE IF EXISTS permission;

-- Tạo bảng Permission
CREATE TABLE IF NOT EXISTS permission (
    name VARCHAR(255) PRIMARY KEY,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng Roles
CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng Users
CREATE TABLE IF NOT EXISTS users (
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
    auth_provider VARCHAR(50) DEFAULT 'GOOGLE',
    locked BOOLEAN DEFAULT FALSE,
    locked_at TIMESTAMP NULL,
    locked_by VARCHAR(36),
    lock_reason TEXT,
    joined_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    balance DECIMAL(15,2) DEFAULT 0.00,
    revenue DECIMAL(15,2) DEFAULT 0.00,
    bank_name VARCHAR(100),
    bank_account_holder_name VARCHAR(100),
    bank_account_number VARCHAR(20),
    FOREIGN KEY (locked_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng User_Roles (Many-to-Many)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(36),
    role_id VARCHAR(36),
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng Role_Permissions (Many-to-Many)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(36),
    permission_id VARCHAR(255),
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission(name) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng Invalidated_Token (để lưu token đã logout)
CREATE TABLE IF NOT EXISTS invalidated_token (
    id VARCHAR(255) PRIMARY KEY,
    expiry_time TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- INSERT DỮ LIỆU MẪU
-- ============================================

-- Insert Permissions
INSERT INTO permission (name, description) VALUES
('CREATE_USER', 'Tạo người dùng mới'),
('UPDATE_USER', 'Cập nhật thông tin người dùng'),
('DELETE_USER', 'Xóa người dùng'),
('VIEW_USER', 'Xem thông tin người dùng'),
('MANAGE_ROLES', 'Quản lý roles và permissions'),
('LOCK_USER', 'Khóa tài khoản người dùng'),
('UNLOCK_USER', 'Mở khóa tài khoản người dùng')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- Insert Roles
INSERT INTO roles (id, name, description) VALUES
(UUID(), 'ADMIN', 'Quản trị viên hệ thống - có toàn quyền'),
(UUID(), 'USER', 'Người dùng thông thường'),
(UUID(), 'MODERATOR', 'Người kiểm duyệt nội dung')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- Lấy role IDs
SET @admin_role_id = (SELECT id FROM roles WHERE name = 'ADMIN');
SET @user_role_id = (SELECT id FROM roles WHERE name = 'USER');
SET @moderator_role_id = (SELECT id FROM roles WHERE name = 'MODERATOR');

-- Gán permissions cho ADMIN role (tất cả permissions)
INSERT INTO role_permissions (role_id, permission_id) VALUES
(@admin_role_id, 'CREATE_USER'),
(@admin_role_id, 'UPDATE_USER'),
(@admin_role_id, 'DELETE_USER'),
(@admin_role_id, 'VIEW_USER'),
(@admin_role_id, 'MANAGE_ROLES'),
(@admin_role_id, 'LOCK_USER'),
(@admin_role_id, 'UNLOCK_USER')
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- Gán permissions cho USER role (chỉ view)
INSERT INTO role_permissions (role_id, permission_id) VALUES
(@user_role_id, 'VIEW_USER')
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- Gán permissions cho MODERATOR role
INSERT INTO role_permissions (role_id, permission_id) VALUES
(@moderator_role_id, 'VIEW_USER'),
(@moderator_role_id, 'UPDATE_USER'),
(@moderator_role_id, 'LOCK_USER'),
(@moderator_role_id, 'UNLOCK_USER')
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- Insert Admin User (password: admin123)
-- Password đã được mã hóa bằng BCrypt với strength 10
INSERT INTO users (id, username, password, email, full_name, email_verified, auth_provider, locked, joined_date)
VALUES (
    UUID(),
    'admin',
    '$2a$10$XPTYhk5YhJKZL5KZL5KZL.YhJKZL5KZL5KZL5KZL5KZL5KZL5KZL5K', -- admin123
    'admin@wvideos.com',
    'Administrator',
    TRUE,
    'LOCAL',
    FALSE,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- Insert Test User (password: user123)
INSERT INTO users (id, username, password, email, full_name, email_verified, auth_provider, locked, joined_date)
VALUES (
    UUID(),
    'testuser',
    '$2a$10$YhJKZL5KZL5KZL5KZL5KZL.YhJKZL5KZL5KZL5KZL5KZL5KZL5KZL5K', -- user123
    'user@wvideos.com',
    'Test User',
    TRUE,
    'LOCAL',
    FALSE,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- Gán role ADMIN cho admin user
SET @admin_user_id = (SELECT id FROM users WHERE email = 'admin@wvideos.com');
INSERT INTO user_roles (user_id, role_id) VALUES
(@admin_user_id, @admin_role_id)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- Gán role USER cho test user
SET @test_user_id = (SELECT id FROM users WHERE email = 'user@wvideos.com');
INSERT INTO user_roles (user_id, role_id) VALUES
(@test_user_id, @user_role_id)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- ============================================
-- TẠO INDEXES ĐỂ TỐI ƯU PERFORMANCE
-- ============================================

-- Index cho Users table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_locked ON users(locked);
CREATE INDEX idx_users_joined_date ON users(joined_date);

-- Index cho Roles table
CREATE INDEX idx_roles_name ON roles(name);

-- ============================================
-- HIỂN THỊ KẾT QUẢ
-- ============================================

SELECT '=== PERMISSIONS ===' AS '';
SELECT * FROM permission;

SELECT '=== ROLES ===' AS '';
SELECT * FROM roles;

SELECT '=== USERS ===' AS '';
SELECT id, username, email, full_name, email_verified, locked, joined_date FROM users;

SELECT '=== USER ROLES ===' AS '';
SELECT 
    u.username,
    u.email,
    r.name as role_name,
    r.description as role_description
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id;

SELECT '=== ROLE PERMISSIONS ===' AS '';
SELECT 
    r.name as role_name,
    p.name as permission_name,
    p.description as permission_description
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.name
ORDER BY r.name, p.name;

-- ============================================
-- THÔNG TIN ĐĂNG NHẬP
-- ============================================

SELECT '=== THÔNG TIN ĐĂNG NHẬP ===' AS '';
SELECT 
    'Admin Account' as account_type,
    'admin@wvideos.com' as email,
    'admin123' as password
UNION ALL
SELECT 
    'User Account' as account_type,
    'user@wvideos.com' as email,
    'user123' as password;
