-- Migration: Thêm column last_synced_at vào bảng videos
-- Mục đích: Theo dõi lần sync cuối cùng với DoodStream API để tối ưu performance

USE db_wvideos;

-- Thêm column last_synced_at
ALTER TABLE videos 
ADD COLUMN last_synced_at DATETIME NULL 
COMMENT 'Timestamp của lần sync thông tin video từ DoodStream gần nhất';

-- Tạo index để tối ưu query theo last_synced_at
CREATE INDEX idx_videos_last_synced_at ON videos(last_synced_at);

-- Verify
DESCRIBE videos;

SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'db_wvideos' 
  AND TABLE_NAME = 'videos' 
  AND COLUMN_NAME = 'last_synced_at';
