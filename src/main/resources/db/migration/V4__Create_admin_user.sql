-- Tạo admin user và roles
-- Password: admin123 (BCrypt hash)

-- Tạo permissions
INSERT IGNORE INTO permissions (name, description) VALUES 
('USER_READ', 'Đọc thông tin người dùng'),
('USER_WRITE', 'Ghi thông tin người dùng'),
('USER_DELETE', 'Xóa người dùng'),
('VIDEO_READ', 'Đọc thông tin video'),
('VIDEO_WRITE', 'Ghi thông tin video'),
('VIDEO_DELETE', 'Xóa video'),
('CATEGORY_READ', 'Đọc thông tin thể loại'),
('CATEGORY_WRITE', 'Ghi thông tin thể loại'),
('CATEGORY_DELETE', 'Xóa thể loại'),
('ADMIN_ACCESS', 'Truy cập admin panel');

-- Tạo roles
INSERT IGNORE INTO roles (name, description) VALUES 
('USER', 'Người dùng thường'),
('ADMIN', 'Quản trị viên');

-- Gán permissions cho roles
INSERT IGNORE INTO role_permissions (role_name, permission_name) VALUES 
-- USER role
('USER', 'USER_READ'),
('USER', 'VIDEO_READ'),
('USER', 'CATEGORY_READ'),

-- ADMIN role (có tất cả permissions)
('ADMIN', 'USER_READ'),
('ADMIN', 'USER_WRITE'),
('ADMIN', 'USER_DELETE'),
('ADMIN', 'VIDEO_READ'),
('ADMIN', 'VIDEO_WRITE'),
('ADMIN', 'VIDEO_DELETE'),
('ADMIN', 'CATEGORY_READ'),
('ADMIN', 'CATEGORY_WRITE'),
('ADMIN', 'CATEGORY_DELETE'),
('ADMIN', 'ADMIN_ACCESS');

-- Tạo admin user
INSERT IGNORE INTO users (
    id, 
    username, 
    email, 
    password, 
    full_name, 
    email_verified, 
    auth_provider, 
    locked, 
    joined_date
) VALUES (
    'admin-user-id-001',
    'admin',
    'admin@wvideos.com',
    '$2a$10$N.zmdr9k7uOCQb96VdodAOBGk07l3.0H3/Gp0NusqeGrxJNOYKqTK', -- admin123
    'Administrator',
    true,
    'LOCAL',
    false,
    NOW()
);

-- Gán role ADMIN cho admin user
INSERT IGNORE INTO user_roles (user_id, role_name) VALUES 
('admin-user-id-001', 'ADMIN');