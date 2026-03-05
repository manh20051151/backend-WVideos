-- Kiểm tra videos trong database
USE db_wvideos;

-- Xem tất cả videos
SELECT 
    id,
    title,
    file_code,
    thumbnail_url,
    splash_image_url,
    views,
    status,
    is_public,
    created_at
FROM videos
ORDER BY created_at DESC;

-- Đếm số lượng videos theo status
SELECT 
    status,
    COUNT(*) as count
FROM videos
GROUP BY status;

-- Xem videos không có thumbnail
SELECT 
    id,
    title,
    file_code,
    thumbnail_url,
    status
FROM videos
WHERE thumbnail_url IS NULL OR thumbnail_url = '';
