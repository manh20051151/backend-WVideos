package com.example.backendWVideos.entity;

import com.example.backendWVideos.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Báo cáo vi phạm video: mỗi user chỉ báo cáo 1 lần cho mỗi video.
 */
@Entity
@Table(
        name = "video_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_video_report_user_video",
                columnNames = {"reporter_user_id", "video_id"}
        ),
        indexes = {
                @Index(name = "idx_video_report_status", columnList = "status"),
                @Index(name = "idx_video_report_video", columnList = "video_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 50)
    private String videoId; // ID video bị báo cáo

    @Column(name = "reporter_user_id", nullable = false, length = 50)
    private String reporterUserId; // ID user báo cáo

    @Column(name = "reason", nullable = false, length = 50)
    private String reason; // Code lý do báo cáo (tham chiếu report_reasons.code)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Mô tả chi tiết (tùy chọn)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt; // Thời điểm admin xử lý

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy; // ID admin xử lý

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote; // Ghi chú của admin khi xử lý
}
