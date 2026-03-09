-- Thêm cột created_by_name vào bảng categories
ALTER TABLE categories ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(255);

-- Cập nhật dữ liệu hiện có: lấy tên từ bảng users
UPDATE categories c
INNER JOIN users u ON c.created_by = u.id
SET c.created_by_name = COALESCE(u.full_name, u.username)
WHERE c.created_by IS NOT NULL AND c.created_by_name IS NULL;
