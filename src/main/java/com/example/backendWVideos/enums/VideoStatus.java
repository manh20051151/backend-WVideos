package com.example.backendWVideos.enums;

public enum VideoStatus {
    UPLOADING,      // Đang upload
    PROCESSING,     // Đang xử lý trên DoodStream
    READY,          // Sẵn sàng xem
    FAILED,         // Upload thất bại
    DELETED         // Đã xóa
}
