# Hướng dẫn Sync thông tin Video từ DoodStream

## Tổng quan

Tính năng sync cho phép cập nhật thông tin video realtime từ DoodStream về database, bao gồm:
- Số lượt xem (views)
- Trạng thái video (status)
- Kích thước file (fileSize)
- Độ dài video (duration)
- Thumbnail và splash image URLs

## API Endpoint

### POST /videos/{videoId}/sync

Sync thông tin video từ DoodStream về database.

**Authentication**: Required (JWT Token)

**Path Parameters**:
- `videoId` (string, required): ID của video cần sync

**Response**:
```json
{
  "code": 1000,
  "result": {
    "id": "uuid",
    "title": "Video title",
    "fileCode": "xxx",
    "views": 123,
    "status": "READY",
    "thumbnailUrl": "https://img.doodcdn.io/snaps/xxx.jpg",
    "splashImageUrl": "https://img.doodcdn.io/splash/xxx.jpg",
    "fileSize": 123456,
    "duration": 300,
    ...
  }
}
```

**Error Codes**:
- `1005`: User not found
- `2001`: Video not found
- `1002`: Unauthorized (không phải chủ sở hữu video)
- `3001`: DoodStream API error

## Cách sử dụng

### 1. Backend (Java)

```java
// VideoService.java
@Transactional
public VideoResponse syncVideoInfo(String userEmail, String videoId) {
    // Kiểm tra quyền sở hữu
    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    
    Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

    if (!video.getUser().getId().equals(user.getId())) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    // Gọi DoodStream API
    Map<String, Object> fileInfo = doodStreamService.getFileInfo(video.getFileCode());
    
    // Cập nhật database
    // ... parse và update video entity
    
    return videoMapper.toVideoResponse(video);
}
```

### 2. Frontend (TypeScript)

```typescript
// video.api.ts
syncVideoInfo: async (videoId: string): Promise<VideoResponse> => {
  const response = await axiosClient.post(`/videos/${videoId}/sync`);
  return response.result;
}

// Sử dụng trong component
const handleSync = async (videoId: string) => {
  try {
    const updatedVideo = await videoApi.syncVideoInfo(videoId);
    console.log('✅ Sync thành công:', updatedVideo);
    // Cập nhật UI với thông tin mới
  } catch (error) {
    console.error('❌ Lỗi khi sync:', error);
  }
};
```

### 3. Thêm nút Sync trong My Videos page

```tsx
// my-videos/page.tsx
<button
  onClick={() => handleSync(video.id)}
  className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
>
  🔄 Refresh
</button>
```

## DoodStream API Reference

### File Info API

**Endpoint**: `GET https://doodapi.co/api/file/info`

**Parameters**:
- `key`: API Key
- `file_code`: File code

**Response**:
```json
{
  "msg": "OK",
  "server_time": "2017-08-11 04:30:07",
  "status": 200,
  "result": [
    {
      "single_img": "https://img.doodcdn.io/snaps/xxx.jpg",
      "status": 200,
      "filecode": "xxx",
      "splash_img": "https://img.doodcdn.io/splash/xxx.jpg",
      "canplay": 1,
      "size": "123456",
      "views": "0",
      "length": "123456",
      "uploaded": "2017-08-11 04:30:07",
      "last_view": "",
      "protected_embed": "/e/yyy",
      "protected_dl": "/d/zzz",
      "title": "test_file"
    }
  ]
}
```

## Use Cases

### 1. Cập nhật views định kỳ

Tạo scheduled job để sync views cho các video hot:

```java
@Scheduled(fixedRate = 3600000) // Mỗi giờ
public void syncPopularVideos() {
    List<Video> popularVideos = videoRepository.findTop100ByOrderByViewsDesc();
    
    for (Video video : popularVideos) {
        try {
            syncVideoInfo(video.getUser().getEmail(), video.getId());
        } catch (Exception e) {
            log.error("Failed to sync video {}: {}", video.getId(), e.getMessage());
        }
    }
}
```

### 2. Kiểm tra trạng thái video sau upload

Sau khi upload, video có thể ở trạng thái PROCESSING. Dùng sync để kiểm tra khi nào video READY:

```typescript
const pollVideoStatus = async (videoId: string) => {
  const maxAttempts = 10;
  let attempts = 0;
  
  while (attempts < maxAttempts) {
    const video = await videoApi.syncVideoInfo(videoId);
    
    if (video.status === 'READY') {
      console.log('✅ Video đã sẵn sàng!');
      return video;
    }
    
    await new Promise(resolve => setTimeout(resolve, 5000)); // Đợi 5s
    attempts++;
  }
  
  throw new Error('Video processing timeout');
};
```

### 3. Refresh thumbnails

Nếu DoodStream tạo thumbnails mới, dùng sync để cập nhật:

```typescript
const refreshThumbnails = async (videoId: string) => {
  const video = await videoApi.syncVideoInfo(videoId);
  
  // Force reload images
  const img = document.querySelector(`img[data-video-id="${videoId}"]`);
  if (img) {
    img.src = video.thumbnailUrl + '?t=' + Date.now();
  }
};
```

## Best Practices

1. **Rate Limiting**: Không sync quá thường xuyên để tránh bị DoodStream rate limit
2. **Error Handling**: Luôn handle errors khi gọi API
3. **User Feedback**: Hiển thị loading state khi đang sync
4. **Caching**: Cache kết quả sync trong vài phút để giảm số lần gọi API
5. **Background Jobs**: Dùng scheduled jobs cho sync hàng loạt thay vì sync từng video

## Troubleshooting

### Lỗi: "DoodStream API error"

**Nguyên nhân**: 
- API key không hợp lệ
- File code không tồn tại
- DoodStream API down

**Giải pháp**:
- Kiểm tra API key trong `application.yaml`
- Verify file code trong database
- Retry sau vài phút

### Lỗi: "Unauthorized"

**Nguyên nhân**: User không phải chủ sở hữu video

**Giải pháp**: Chỉ cho phép user sync video của chính họ

### Views không cập nhật

**Nguyên nhân**: DoodStream có thể cache views trong vài phút

**Giải pháp**: Đợi 5-10 phút rồi sync lại

## Tham khảo

- [DoodStream API Documentation](https://doodstream.com/api-docs)
- [VIDEO_API_GUIDE.md](./VIDEO_API_GUIDE.md)
- [GOOGLE_OAUTH2_SETUP.md](./GOOGLE_OAUTH2_SETUP.md)
