package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Bản dịch tự động (Gemini) của TÊN danh mục (thể loại video + danh mục tin tức)
 * sang ngôn ngữ khác. Tên gốc tiếng Việt nằm trên bảng categories / news_categories.
 *
 * - ownerType: "video" = thể loại video, "news" = danh mục tin tức.
 * - ownerId: id của category tương ứng.
 */
@Entity
@Table(name = "category_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_type", "owner_id", "locale"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "owner_type", nullable = false, length = 10)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    // Mã ngôn ngữ BCP-47: en, zh-CN, ja, ko, hi, th, lo, km...
    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
