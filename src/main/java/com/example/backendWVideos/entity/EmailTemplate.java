package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Nội dung email do admin chỉnh sửa (key: CONFIRMATION / RESET_PASSWORD).
 * Nếu không có row trong DB, hệ thống dùng template mặc định trong code.
 */
@Entity
@Table(name = "email_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "template_key", nullable = false, unique = true)
    String templateKey;

    @Column(nullable = false)
    String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    String body;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
