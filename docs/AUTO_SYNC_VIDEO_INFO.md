# Auto-Sync Video Info từ DoodStream

## Tổng quan

Hệ thống **tự động sync thông tin video** từ DoodStream mỗi khi user request danh sách video, không cần click nút Refresh.

## Cơ chế hoạt động

### 1. Smart Sync Strategy

Backend chỉ sync những video **thực sự cần thiết** để tối ưu performance:

```java
private boolean shouldSyncVideo(Video video) {
    // Sync nếu:
    // 1. Video đang PROCESSING hoặc UPLOADING
    // 2. Video chưa có splash image
    // 3. Video READY nhưng views = 0 (chưa sync lần nào)
    return video.getStatus() == VideoStatus.PROCESSING 
        || video.getStatus() == VideoStatus.UPLOADING
        || video.getSplashImageUrl() == null 
        || video.getSplashImageUrl().isEmpty()
        || (video.getStatus() == VideoStatus.READY && video.getViews() == 0);
}
```

### 2. Silent Sync

Sync được thực hiện **im lặng** (silent mode):
- ✅ Không throw exception nếu DoodStream API fail
- ✅ Dùng data cũ trong database nếu sync thất bại
- ✅ Log warning nhưng không ảnh hưởng user experience

```java
private void syncVideoInfoSilent(Video video) {
    try {
        // Gọi DoodStream API
        Map<String, Object> fileInfo = doodStreamService.getFileInfo(video.getFileCode());
        
        // Cập nhật database
        // ...
        
        log.debug("✅ Auto-sync video {}: {} views", video.getId(), video.getViews());
    } catch (Exception e) {
        log.debug("⚠️ Skip sync for video {}: {}", video.getId(), e.getMessage());
        // Không throw exception
    }
}
```

### 3. Transaction Management

Method `getMyVideos` sử dụng `@Transactional` (không phải `readOnly`) để cho phép save:

```java
@Transactional
public Page<VideoResponse> getMyVideos(String userEmail, Pageable pageable) {
    // Lấy videos
    Page<Video> videos = videoRepository.findByUserId(user.getId(), pageable);
    
    // Auto-sync videos cần thiết
    videos.forEach(video -> {
        if (shouldSyncVideo(video)) {
            syncVideoInfoSilent(video);
        }
    });
    
    return videos.map(videoMapper::toVideoResponse);
}
```

## Lợi ích

### 1. User Experience tốt hơn

- ✅ Không cần click nút Refresh
- ✅ Thông tin luôn mới nhất
- ✅ Thumbnail tự động hiển thị khi DoodStream xử lý xong

### 2. Performance tối ưu

- ✅ Chỉ sync video cần thiết (không phải tất cả)
- ✅ Video READY với thumbnail đã có → skip sync
- ✅ Giảm số lượng API calls đến DoodStream

### 3. Reliability cao

- ✅ Không bị crash nếu DoodStream API fail
- ✅ Fallback về data cũ trong database
- ✅ Log warning để debug

## Use Cases

### Case 1: Video mới upload

```
1. User upload video → status = UPLOADING
2. User vào trang "Video của tôi"
3. Backend auto-sync → DoodStream trả về status = PROCESSING
4. Database cập nhật status = PROCESSING
5. User refresh trang sau 2 phút
6. Backend auto-sync → DoodStream trả về status = READY + thumbnails
7. Database cập nhật status = READY + thumbnails
8. User thấy thumbnail hiển thị ✅
```

### Case 2: Video đã READY

```
1. User vào trang "Video của tôi"
2. Backend check: video READY + có splash image + views > 0
3. Skip sync (dùng data trong database)
4. Response nhanh ✅
```

### Case 3: DoodStream API fail

```
1. User vào trang "Video của tôi"
2. Backend auto-sync → DoodStream API timeout
3. Log warning: "⚠️ Không thể sync video xxx"
4. Dùng data cũ trong database
5. User vẫn thấy danh sách video (không bị lỗi) ✅
```

## Performance Analysis

### Scenario 1: User có 10 videos

- 5 videos READY + có thumbnail → skip sync
- 3 videos READY + chưa có thumbnail → sync (3 API calls)
- 2 videos PROCESSING → sync (2 API calls)
- **Total**: 5 API calls thay vì 10

### Scenario 2: User có 100 videos (pagination = 12/page)

- Page 1: 12 videos
  - 8 videos skip sync
  - 4 videos sync
  - **Total**: 4 API calls
- Response time: ~2-3 giây (acceptable)

### Scenario 3: DoodStream API slow

- Timeout: 5 giây
- Nếu 1 video timeout → skip và tiếp tục
- Không block toàn bộ request

## Configuration

### Tối ưu thêm (optional)

Nếu muốn giảm số lượng sync hơn nữa, có thể thêm cache:

```java
// Chỉ sync nếu chưa sync trong 5 phút gần đây
private boolean shouldSyncVideo(Video video) {
    if (video.getLastSyncedAt() != null) {
        long minutesSinceLastSync = ChronoUnit.MINUTES.between(
            video.getLastSyncedAt(), 
            LocalDateTime.now()
        );
        
        if (minutesSinceLastSync < 5) {
            return false; // Skip sync
        }
    }
    
    // ... existing logic
}
```

Cần thêm column `last_synced_at` vào table `videos`.

## Monitoring

### Backend logs

```
DEBUG: ✅ Auto-sync video xxx: 123 views, status=READY
DEBUG: ⚠️ Skip sync for video yyy: Connection timeout
WARN:  ⚠️ Không thể sync video zzz: Invalid file_code
```

### Metrics cần track

- Số lượng videos sync per request
- Success rate của DoodStream API
- Average response time của getMyVideos
- Số lượng videos skip sync

## Troubleshooting

### Issue 1: Response chậm

**Nguyên nhân**: Quá nhiều videos cần sync

**Giải pháp**:
1. Tăng pagination size (hiện tại: 12)
2. Thêm cache last_synced_at
3. Giảm timeout cho DoodStream API

### Issue 2: Thumbnail không cập nhật

**Nguyên nhân**: Video đã có splash image nên skip sync

**Giải pháp**:
- Xóa splash_image_url trong database
- Hoặc thêm manual sync endpoint

### Issue 3: DoodStream API rate limit

**Nguyên nhân**: Quá nhiều requests

**Giải pháp**:
1. Thêm rate limiting trong DoodStreamService
2. Implement exponential backoff
3. Cache kết quả sync

## Best Practices

### 1. Logging

```java
// DEBUG level cho normal operations
log.debug("✅ Auto-sync video {}: {} views", video.getId(), video.getViews());

// WARN level cho errors
log.warn("⚠️ Không thể sync video {}: {}", video.getId(), e.getMessage());
```

### 2. Error Handling

```java
try {
    syncVideoInfoSilent(video);
} catch (Exception e) {
    // Log nhưng không throw
    log.warn("⚠️ Sync failed: {}", e.getMessage());
}
```

### 3. Transaction Management

```java
// Dùng @Transactional (không phải readOnly)
@Transactional
public Page<VideoResponse> getMyVideos(...) {
    // ...
}
```

## Tham khảo

- [VIDEO_SYNC_GUIDE.md](./VIDEO_SYNC_GUIDE.md) - Manual sync API
- [VIDEO_API_GUIDE.md](./VIDEO_API_GUIDE.md) - Video API documentation
- [DoodStream API Docs](https://doodstream.com/api-docs)
