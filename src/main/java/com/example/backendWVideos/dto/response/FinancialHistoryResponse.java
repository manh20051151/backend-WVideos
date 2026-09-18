package com.example.backendWVideos.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tổng hợp biến động tài chính của người dùng:
 * số dư, doanh thu, danh sách giao dịch và thống kê theo tháng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialHistoryResponse {

    private Double balance;          // Số dư hiện tại (VND)
    private Double revenue;          // Tổng doanh thu lũy kế (VND)
    private Double totalDeposited;   // Tổng đã nạp qua ví (VND)
    private Double totalSpent;       // Tổng đã chi mua video (VND)
    private Double deposited30d;     // Nạp trong 30 ngày gần nhất
    private Double spent30d;         // Chi trong 30 ngày gần nhất
    private Double revenue30d;       // Doanh thu 30 ngày gần nhất

    private List<FinancialEvent> events;       // Biến động mới nhất trước (giới hạn bản ghi)
    private List<MonthlyStat> monthlyStats;    // Các tháng có dữ liệu, tối đa 36 tháng (cũ -> mới)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialEvent {
        private String id;
        private String type;          // TOPUP | VIDEO_PURCHASE | CREATOR_REVENUE
        private String title;         // Nhãn chính (VD: "Nạp tiền qua ngân hàng")
        private String description;   // Mô tả chi tiết (VD: tên video, nội dung chuyển khoản)
        private Double amount;        // Giá trị tuyệt đối (VND), luôn > 0
        private String direction;     // IN (tăng) | OUT (giảm)
        private LocalDateTime occurredAt;
        private String videoId;       // Gắn với video nếu là giao dịch mua / doanh thu
        private String videoSlug;     // Slug video cho link /watch/{slug}
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStat {
        private String month;   //yyyy-MM
        private Double deposits;
        private Double spending;
        private Double earnings;
    }
}
