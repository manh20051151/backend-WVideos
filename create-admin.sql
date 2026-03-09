-- Tạo admin user để test
-- Username: admin, Password: admin123

-- Tạo permissions nếu chưa có
INSERT IGNORE INTO permissions (name, description) VALUES 
('ADMIN_ACCESS', 'Truy cập admin panel'),
('CATEGORY_WRITE', 'Ghi thông tin thể loại');

-- Tạo role ADMIN nếu chưa có
INSERT IGNORE INTO roles (name, description) VALUES 
('ADMIN', 'Quản trị viên');

-- Gán permissions cho role ADMIN
INSERT IGNORE INTO role_permissions (role_name, permission_name) VALUES 
('ADMIN', 'ADMIN_ACCESS'),
('ADMIN', 'CATEGORY_WRITE');

-- Tạo admin user (password: admin123 -> BCrypt hash)
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
    'admin-001',
    'admin',
    'admin@wvideos.com',
    '$2a$10$N.zmdr9k7uOCQb96VdodAOBGk07l3.0H3/Gp0NusqeGrxJNOYKqTK',
    'Administrator',
    true,
    'LOCAL',
    false,
    NOW()
);

-- Gán role ADMIN cho user
INSERT IGNORE INTO user_roles (user_id, role_name) VALUES 
('admin-001', 'ADMIN');

-- Kiểm tra kết quả
SELECT 'Admin user created successfully' as result;
SELECT u.username, u.email, r.name as role_name 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_name = r.name 
WHERE u.username = 'admin';