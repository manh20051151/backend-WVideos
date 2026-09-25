package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Bản dịch tự động (Gemini) của tiêu đề + mô tả video sang ngôn ngữ khác.
 * Ngôn ngữ gốc là tiếng Việt (title/description trên bảng videos).
 */
@Entity
@Table(name = "video_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"video_id", "locale"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    // Mã ngôn ngữ BCP-47: en, zh-CN, ja, ko, hi, th, lo, km...
    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
