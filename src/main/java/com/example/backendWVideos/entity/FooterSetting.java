package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "footer_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FooterSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey; // Khóa cấu hình (VD: "brand_description", "copyright_text")

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue; // Giá trị cấu hình

    @Column(name = "is_active")
    private Boolean isActive = true; // Có sử dụng cấu hình này không

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Người tạo (admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Tên người tạo (lưu trực tiếp để dễ truy vấn)
    @Column(name = "created_by_name")
    private String createdByName;
}