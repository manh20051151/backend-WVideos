-- Tạo bảng trung gian video_categories cho quan hệ ManyToMany
CREATE TABLE IF NOT EXISTS video_categories (
    video_id VARCHAR(255) NOT NULL,
    category_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (video_id, category_id),
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Migrate dữ liệu từ cột category_id cũ sang bảng video_categories
INSERT INTO video_categories (video_id, category_id)
SELECT id, category_id
FROM videos
WHERE category_id IS NOT NULL;

-- Xóa cột category_id cũ
ALTER TABLE videos DROP COLUMN IF EXISTS category_id;
