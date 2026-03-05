# Hướng dẫn sử dụng Video API

## Tổng quan
API cho phép người dùng upload, quản lý và xem video thông qua tích hợp DoodStream.

## Cấu hình

### 1. Thêm DoodStream API Key

Thêm vào file `.env` hoặc `application.yaml`:

```yaml
doodstream:
  api-key: your_doodstream_api_key_here
```

Lấy API key tại: https://doodstream.com/settings

### 2. Database Migration

Video entity sẽ tự động tạo bảng `videos` khi chạy ứng dụng (ddl-auto: update).

## API Endpoints

### 1. Upload Video

**POST** `/api/videos/upload`

Upload video lên DoodStream.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: multipart/form-data
```

**Request:**
```
file: (video file)
data: {
  "title": "Tiêu đề video",
  "description": "Mô tả video",
  "isPublic": true
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "id": "uuid",
    "title": "Tiêu đề video",
    "description": "Mô tả video",
    "fileCode": "xxx",
    "downloadUrl": "https://dood.to/d/xxx",
    "embedUrl": "https://dood.to/e/xxx",
    "thumbnailUrl": "https://img.doodcdn.io/snaps/xxx.jpg",
    "fileSize": 123456789,
    "duration": 300,
    "views": 0,
    "status": "READY",
    "isPublic": true,
    "userId": "user-uuid",
    "username": "username",
    "createdAt": "2024-01-30T10:00:00",
    "uploadedToDoodStreamAt": "2024-01-30T10:05:00"
  }
}
```

**Video Status:**
- `UPLOADING`: Đang upload
- `PROCESSING`: Đang xử lý trên DoodStream
- `READY`: Sẵn sàng xem
- `FAILED`: Upload thất bại
- `DELETED`: Đã xóa

### 2. Lấy danh sách video của tôi

**GET** `/api/videos/my-videos`

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Query Parameters:**
- `page`: Số trang (default: 0)
- `size`: Số lượng/trang (default: 10)
- `sortBy`: Sắp xếp theo (default: createdAt)
- `sortDir`: Hướng sắp xếp ASC/DESC (default: DESC)

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [...],
    "totalElements": 50,
    "totalPages": 5,
    "size": 10,
    "number": 0
  }
}
```

### 3. Lấy danh sách video công khai

**GET** `/api/videos/public`

Không cần authentication.

**Query Parameters:**
- `page`: Số trang (default: 0)
- `size`: Số lượng/trang (default: 10)
- `sortBy`: Sắp xếp theo (default: createdAt)
- `sortDir`: Hướng sắp xếp ASC/DESC (default: DESC)

### 4. Lấy chi tiết video

**GET** `/api/videos/{videoId}`

**Headers:**
```
Authorization: Bearer {jwt_token}
```

### 5. Cập nhật video

**PUT** `/api/videos/{videoId}`

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request:**
```json
{
  "title": "Tiêu đề mới",
  "description": "Mô tả mới",
  "isPublic": false
}
```

### 6. Xóa video

**DELETE** `/api/videos/{videoId}`

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Note:** Video sẽ được đánh dấu là DELETED, không xóa khỏi database.

### 7. Tăng lượt xem

**POST** `/api/videos/{videoId}/view`

Không cần authentication.

## Validation Rules

### Upload Video
- **File:**
  - Bắt buộc phải có file
  - Chỉ chấp nhận video files (video/*)
  - Kích thước tối đa: 2GB

- **Title:**
  - Bắt buộc
  - Độ dài: 3-200 ký tự

- **Description:**
  - Không bắt buộc
  - Độ dài tối đa: 2000 ký tự

## Error Codes

| Code | Message | HTTP Status |
|------|---------|-------------|
| 9601 | Không tìm thấy video | 404 |
| 9602 | File không được để trống | 400 |
| 9603 | Lỗi kết nối DoodStream | 500 |
| 9604 | Upload video thất bại | 500 |
| 9203 | File quá lớn (tối đa 2GB) | 400 |
| 9204 | Loại file không được hỗ trợ | 400 |
| 1007 | Không có quyền | 403 |

## Luồng Upload Video

```
1. User chọn file video và điền thông tin
   ↓
2. Frontend gọi POST /api/videos/upload
   ↓
3. Backend validate file
   ↓
4. Tạo Video record với status UPLOADING
   ↓
5. Gọi DoodStream API để lấy upload server
   ↓
6. Upload file lên DoodStream
   ↓
7. Nhận response từ DoodStream (fileCode, URLs, thumbnails)
   ↓
8. Cập nhật Video record với thông tin từ DoodStream
   ↓
9. Set status = READY hoặc PROCESSING
   ↓
10. Return VideoResponse cho frontend
```

## Embed Video trong Frontend

### HTML Embed
```html
<iframe 
  src="https://dood.to/e/{fileCode}" 
  width="640" 
  height="360" 
  frameborder="0" 
  allowfullscreen>
</iframe>
```

### React Component
```jsx
const VideoPlayer = ({ embedUrl }) => {
  return (
    <div className="video-container">
      <iframe
        src={embedUrl}
        width="100%"
        height="500px"
        frameBorder="0"
        allowFullScreen
      />
    </div>
  );
};
```

## Best Practices

1. **Upload Progress:**
   - Hiển thị progress bar khi upload
   - Cho phép cancel upload
   - Hiển thị thông báo khi upload thành công/thất bại

2. **Video Player:**
   - Sử dụng embed URL từ DoodStream
   - Hiển thị thumbnail trước khi play
   - Tăng view count khi user bắt đầu xem

3. **Performance:**
   - Lazy load video thumbnails
   - Pagination cho danh sách video
   - Cache video metadata

4. **Security:**
   - Validate file type và size ở cả frontend và backend
   - Kiểm tra quyền sở hữu khi update/delete
   - Rate limiting cho upload API

## Testing

### Test Upload Video
```bash
curl -X POST http://localhost:8080/api/videos/upload \
  -H "Authorization: Bearer {token}" \
  -F "file=@video.mp4" \
  -F 'data={"title":"Test Video","description":"Test","isPublic":true}'
```

### Test Get Public Videos
```bash
curl http://localhost:8080/api/videos/public?page=0&size=10
```

## Troubleshooting

### Lỗi: DOODSTREAM_ERROR
- Kiểm tra API key có đúng không
- Kiểm tra kết nối internet
- Xem logs để biết chi tiết lỗi

### Lỗi: FILE_TOO_LARGE
- Giảm kích thước file
- Nén video trước khi upload

### Lỗi: UPLOAD_FAILED
- Thử upload lại
- Kiểm tra định dạng file
- Kiểm tra logs backend

## Monitoring

### Metrics cần theo dõi:
- Số lượng video upload/ngày
- Tỷ lệ upload thành công/thất bại
- Thời gian upload trung bình
- Tổng dung lượng video
- Top videos có nhiều views nhất

### Logs quan trọng:
- Upload start/success/failed
- DoodStream API calls
- Video status changes
- User actions (view, update, delete)
