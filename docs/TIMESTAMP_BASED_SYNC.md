# Timestamp-Based Video Sync Strategy

## Tổng quan

Hệ thống sử dụng **timestamp-based sync** để tự động cập nhật thông tin video từ DoodStream API một cách thông minh, tránh gọi API không cần thiết.

## Vấn đề cũ

### Logic cũ (Views-based):
```java
// Chỉ sync nếu:
// 1. Video PROCESSING/UPLOADING
// 2. Chưa có splash image
// 3. Video READY nhưng views = 0
```

### Hạn chế:
- ❌ Video READY với thumbnail và views > 0 sẽ **không bao giờ được sync nữa**
- ❌ Nếu views trên DoodStream thay đổi (10 → 100), database không cập nhật
- ❌ Không thể cập nhật status mới (ví dụ: video bị xóa trên DoodStream)

## Giải pháp mới

### Logic mới (Timestamp-based):
```java
// Sync nếu:
// 1. Video đang PROCESSING/UPLOADING (luôn sync)
// 2. Chưa có splash image (cần lấy thumbnail)
// 3. Chưa sync lần nào (lastSyncedAt == null)
// 4. Đã sync nhưng quá 5 phút (cập nhật views, status mới)
```

### Ưu điểm:
- ✅ Video READY vẫn được sync định kỳ (mỗi 5 phút)
- ✅ Views, status luôn được cập nhật từ DoodStream
- ✅ Giảm số lượng API calls không cần thiết
- ✅ Có thể config sync interval (5 phút, 10 phút, 1 giờ)

## Implementation

### 1. Database Schema

```sql
ALTER TABLE videos 
ADD COLUMN last_synced_at DATETIME NULL 
COMMENT 'Timestamp của lần sync thông tin video từ DoodStream gần nhất';

CREATE INDEX idx_videos_last_synced_at ON videos(last_synced_at);
```

### 2. Entity Field

```java
@Column(name = "last_synced_at")
private LocalDateTime lastSyncedAt;
```

### 3. Sync Logic

```java
private boolean shouldSyncVideo(Video video) {
    if (video.getFileCode() == null || video.getFileCode().isEmpty()) {
        return false;
    }
    
    // 1. Video đang PROCESSING hoặc UPLOADING - luôn sync
    if (video.getStatus() == VideoStatus.PROCESSING || video.getStatus() == VideoStatus.UPLOADING) {
        return true;
    }
    
    // 2. Video chưa có splash image - cần sync để lấy thumbnail
    if (video.getSplashImageUrl() == null || video.getSplashImageUrl().isEmpty()) {
        return true;
    }
    
    // 3. Chưa sync lần nào - cần sync
    if (video.getLastSyncedAt() == null) {
        return true;
    }
    
    // 4. Đã sync nhưng quá 5 phút - sync lại để cập nhật views, status
    LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
    return video.getLastSyncedAt().isBefore(fiveMinutesAgo);
}
```

### 4. Update Timestamp

```java
private void syncVideoInfoSilent(Video video) {
    try {
        // ... sync logic ...
        
        // Cập nhật timestamp sync
        video.setLastSyncedAt(LocalDateTime.now());
        videoRepository.save(video);
        
    } catch (Exception e) {
        log.debug("⚠️ Skip sync for video {}: {}", video.getId(), e.getMessage());
    }
}
```

## Sync Interval Configuration

Hiện tại: **5 phút**

Có thể config trong `application.yaml`:

```yaml
video:
  sync:
    interval-minutes: 5  # Sync mỗi 5 phút
```

### Gợi ý interval:
- **1-5 phút**: Video mới upload, cần cập nhật nhanh
- **10-30 phút**: Video đã READY, cập nhật views định kỳ
- **1-6 giờ**: Video cũ, ít thay đổi

## Ví dụ Scenarios

### Scenario 1: Video mới upload
```
Upload → UPLOADING (sync ngay)
→ PROCESSING (sync mỗi 5 phút)
→ READY (sync mỗi 5 phút)
→ Sau 1 giờ: vẫn sync mỗi 5 phút
```

### Scenario 2: Video READY với views tăng
```
T0: Video READY, views = 10, lastSyncedAt = 10:00
T1 (10:04): User xem video → không sync (chưa đủ 5 phút)
T2 (10:06): User load page → sync (đã quá 5 phút)
→ Views cập nhật từ 10 → 15
→ lastSyncedAt = 10:06
```

### Scenario 3: Video bị xóa trên DoodStream
```
T0: Video READY, lastSyncedAt = 10:00
T1 (10:06): Sync → DoodStream trả về error
→ Có thể set status = DELETED hoặc UNAVAILABLE
```

## Performance Impact

### API Calls:
- **Trước**: Chỉ sync video PROCESSING/UPLOADING/no-thumbnail
- **Sau**: Sync tất cả video mỗi 5 phút

### Tối ưu:
- Sử dụng `syncVideoInfoSilent()` - không throw exception
- Chỉ sync khi user load page (không có background job)
- Index trên `last_synced_at` để query nhanh

### Estimate:
- 100 videos READY
- Mỗi user load page: sync ~20 videos (những video quá 5 phút)
- DoodStream API: ~20 calls/request

## Testing

### Test Cases:

1. **Video mới upload**
   - lastSyncedAt = null → sync ngay
   
2. **Video PROCESSING**
   - Luôn sync (bất kể lastSyncedAt)
   
3. **Video READY, chưa có thumbnail**
   - Sync để lấy splash image
   
4. **Video READY, có thumbnail, lastSyncedAt = 2 phút trước**
   - Không sync (chưa đủ 5 phút)
   
5. **Video READY, có thumbnail, lastSyncedAt = 10 phút trước**
   - Sync (đã quá 5 phút)

## Migration Guide

### Bước 1: Chạy SQL migration
```bash
mysql -u root -p db_wvideos < scripts/sql/add-last-synced-at-column.sql
```

### Bước 2: Restart backend
```bash
mvn spring-boot:run
```

### Bước 3: Verify
- Upload video mới → check lastSyncedAt được set
- Load my-videos page → check videos được sync
- Check logs: `✅ Auto-sync video {id}: {views} views`

## Troubleshooting

### Video không sync
- Check `lastSyncedAt` trong database
- Check logs: `⚠️ Skip sync for video {id}`
- Verify DoodStream API key

### Sync quá nhiều lần
- Tăng interval từ 5 phút → 10 phút
- Check logic `shouldSyncVideo()`

### Views không cập nhật
- Check DoodStream API response
- Verify `syncVideoInfoSilent()` logic
- Check `lastSyncedAt` có được update không

## Future Improvements

1. **Config sync interval theo video status**
   - PROCESSING: 1 phút
   - READY (< 1 ngày): 5 phút
   - READY (> 1 ngày): 1 giờ

2. **B