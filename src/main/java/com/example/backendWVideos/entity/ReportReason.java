package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lý do báo cáo video - admin có thể CRUD (thêm/sửa/xóa/kích hoạt).
 * `code` dùng làm giá trị lưu trong video_reports.reason, `label` là text hiển thị.
 */
@Entity
@Table(
        name = "report_reasons",
        uniqueConstraints = @UniqueConstraint(name = "uk_report_reason_code", columnNames = "code")
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code; // Mã định danh (VD: SPAM) - dùng để lưu trong báo cáo

    @Column(name = "label", nullable = false, length = 255)
    private String label; // Nhãn hiển thị cho người dùng

    @Column(name = "icon", columnDefinition = "TEXT")
    private String icon; // Đường dẫn SVG path (tùy chọn) - frontend render icon

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0; // Thứ tự hiển thị

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Ẩn khỏi modal báo cáo khi false

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
