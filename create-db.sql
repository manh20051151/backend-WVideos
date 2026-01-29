-- Tạo database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS db_wvideos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE db_wvideos;

-- Xóa tables cũ nếu tồn tại (theo thứ tự dependency)
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS invalidated_token;
DROP TABLE IF EXISTS pending_registration;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS permissions;

-- Tạo bảng permissions
CREATE TABLE permissions (
    name VARCHAR(255) PRIMARY KEY,
    description VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng roles
CREATE TABLE roles (
    name VARCHAR(255) PRIMARY KEY,
    description VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng role_permissions
CREATE TABLE role_permissions (
    role_name VARCHAR(255) NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (role_name, permission_name),
    FOREIGN KEY (role_name) REFERENCES roles(name) ON DELETE CASCADE,
    FOREIGN KEY (permission_name) REFERENCES permissions(name) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng users
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255),
    number_phone VARCHAR(20),
    full_name VARCHAR(255),
    dob DATE,
    avatar TEXT,
    email VARCHAR(255) UNIQUE,
    email_verified BOOLEAN DEFAULT FALSE,
    gender BOOLEAN,
    auth_provider VARCHAR(50) DEFAULT 'LOCAL',
    locked BOOLEAN DEFAULT FALSE,
    locked_at DATETIME,
    locked_by VARCHAR(36),
    lock_reason TEXT,
    joined_date DATETIME,
    balance DECIMAL(15,2) DEFAULT 0.00,
    revenue DECIMAL(15,2) DEFAULT 0.00,
    bank_name VARCHAR(100),
    bank_account_holder_name VARCHAR(100),
    bank_account_number VARCHAR(20),
    FOREIGN KEY (locked_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng user_roles
CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(name) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng invalidated_token
CREATE TABLE invalidated_token (
    id VARCHAR(36) PRIMARY KEY,
    expiry_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng pending_registration
CREATE TABLE pending_registration (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255),
    number_phone VARCHAR(20),
    full_name VARCHAR(255),
    email VARCHAR(255),
    token VARCHAR(255),
    auth_provider VARCHAR(50),
    expiry_date DATETIME NOT NULL,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert permissions
INSERT INTO permissions (name, description) VALUES
('CREATE_USER', 'Tạo người dùng mới'),
('UPDATE_USER', 'Cập nhật thông tin người dùng'),
('DELETE_USER', 'Xóa người dùng'),
('VIEW_USER', 'Xem thông tin người dùng');

-- Insert roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Quản trị viên hệ thống'),
('USER', 'Người dùng thông thường'),
('GUEST', 'Khách');

-- Insert role_permissions
INSERT INTO role_permissions (role_name, permission_name) VALUES
('ADMIN', 'CREATE_USER'),
('ADMIN', 'UPDATE_USER'),
('ADMIN', 'DELETE_USER'),
('ADMIN', 'VIEW_USER'),
('USER', 'VIEW_USER');

-- Insert admin user (password: admin123)
INSERT INTO users (id, username, password, email, full_name, auth_provider, email_verified, locked, balance, revenue, joined_date)
VALUES (
    UUID(),
    'admin',
    '$2a$10$8S5qPJQfQqZQZQZQZQZQZeN5vXKp7mXKp7mXKp7mXKp7mXKp7mXKp',
    'admin@wvideos.com',
    'Administrator',
    'LOCAL',
    TRUE,
    FALSE,
    0.00,
    0.00,
    NOW()
);

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin';

SELECT 'Database db_wvideos created successfully!' AS message;
