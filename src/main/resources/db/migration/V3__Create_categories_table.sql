-- Tạo bảng categories nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    color VARCHAR(7), -- Hex color code như #FF0000
    icon VARCHAR(50), -- Emoji hoặc icon class
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    
    -- Foreign key constraint
    CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Kiểm tra và thêm cột category_id nếu chưa tồn tại
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                     WHERE TABLE_SCHEMA = DATABASE() 
                     AND TABLE_NAME = 'videos' 
                     AND COLUMN_NAME = 'category_id');

SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE videos ADD COLUMN category_id VARCHAR(36)', 
              'SELECT "Column category_id already exists" AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Kiểm tra và thêm foreign key constraint nếu chưa tồn tại
SET @fk_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
                 WHERE TABLE_SCHEMA = DATABASE() 
                 AND TABLE_NAME = 'videos' 
                 AND CONSTRAINT_NAME = 'fk_videos_category');

SET @sql = IF(@fk_exists = 0, 
              'ALTER TABLE videos ADD CONSTRAINT fk_videos_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL', 
              'SELECT "Foreign key fk_videos_category already exists" AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Tạo index cho performance (MySQL sẽ bỏ qua nếu đã tồn tại)
CREATE INDEX IF NOT EXISTS idx_categories_active_sort ON categories(is_active, sort_order);
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_videos_category ON videos(category_id);

-- Insert một số thể loại mặc định (chỉ insert nếu chưa tồn tại)
INSERT IGNORE INTO categories (id, name, slug, description, color, icon, is_active, sort_order) VALUES
(UUID(), 'Giải trí', 'giai-tri', 'Video giải trí, vui nhộn', '#FF6B6B', '🎭', TRUE, 1),
(UUID(), 'Giáo dục', 'giao-duc', 'Video học tập, kiến thức', '#4ECDC4', '📚', TRUE, 2),
(UUID(), 'Âm nhạc', 'am-nhac', 'Video âm nhạc, ca nhạc', '#45B7D1', '🎵', TRUE, 3),
(UUID(), 'Thể thao', 'the-thao', 'Video thể thao, thi đấu', '#96CEB4', '⚽', TRUE, 4),
(UUID(), 'Công nghệ', 'cong-nghe', 'Video về công nghệ, lập trình', '#FFEAA7', '💻', TRUE, 5),
(UUID(), 'Du lịch', 'du-lich', 'Video du lịch, khám phá', '#DDA0DD', '✈️', TRUE, 6),
(UUID(), 'Ẩm thực', 'am-thuc', 'Video nấu ăn, món ngon', '#FFB347', '🍳', TRUE, 7),
(UUID(), 'Hài kịch', 'hai-kich', 'Video hài hước, comedy', '#FF69B4', '😂', TRUE, 8);