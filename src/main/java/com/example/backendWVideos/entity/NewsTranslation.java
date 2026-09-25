package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Bản dịch tự động (Gemini) của bài tin tức (tiêu đề + tóm tắt + nội dung HTML)
 * sang ngôn ngữ khác. Bản gốc tiếng Việt nằm trên bảng news.
 */
@Entity
@Table(name = "news_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"news_id", "locale"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    // Mã ngôn ngữ BCP-47: en, zh-CN, ja, ko, hi, th, lo, km...
    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
