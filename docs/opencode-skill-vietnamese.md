# Skill: Comment và Log bằng Tiếng Việt

## Mô tả
Skill này hướng dẫn opencode viết comment code và log bằng Tiếng Việt thay vì Tiếng Anh.

## Hướng dẫn

### Comment code
- Viết comment bằng Tiếng Việt có dấu
- Comment should explain WHAT the code does, not HOW
- Use clear, concise Vietnamese sentences
- Example: `// Lấy danh sách video trending` thay vì `// Get trending videos`

### Log messages
- Tất cả log messages (log.info, log.debug, log.warn, log.error) phải viết bằng Tiếng Việt
- Nội dung log cần rõ ràng, mô tả what happened
- Example: `log.info("Đã xử lý giao dịch thành công: {}", transactionId)` thay vì `log.info("Transaction processed successfully: {}", transactionId)`

### Các từ khóa thường dùng
- "Lấy" / "Get" → "Lấy danh sách", "Lấy thông tin"
- "Cập nhật" / "Update" → "Cập nhật trạng thái", "Cập nhật số dư"
- "Thêm" / "Add" → "Thêm mới", "Thêm vào"
- "Xóa" / "Delete" → "Xóa bỏ", "Loại bỏ"
- "Kiểm tra" / "Check" → "Kiểm tra điều kiện", "Xác minh"
- "Xử lý" / "Process" → "Xử lý yêu cầu", "Xử lý giao dịch"
- "Thành công" / "Success" → "thành công", "hoàn tất"
- "Thất bại" / "Failed" → "thất bại", "lỗi"
- "Đang" / "Loading" → "đang xử lý", "đang tải"

## Áp dụng
- Java/Spring Boot: log.info, log.error, etc.
- TypeScript/Next.js: console.log, console.error
- Any language: comments and logging statements