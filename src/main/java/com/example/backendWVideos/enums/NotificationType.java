package com.example.backendWVideos.enums;

public enum NotificationType {
    COMMENT,       // Có bình luận mới trên video của bạn
    SUBSCRIBE,     // Có người đăng ký kênh của bạn
    PURCHASE,      // Có người mua video có phí của bạn
    LIKE,          // Có người thích video của bạn
    NEW_VIDEO,     // Kênh bạn đã đăng ký có video mới
    ANNOUNCEMENT,  // Thông báo từ quản trị viên gửi đến người dùng
    COMMENT_BANNED // Người dùng bị khóa quyền bình luận (admin)
}
