package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Cấu hình branding của site (logo, favicon) do admin chỉnh sửa.
 * Nếu không có giá trị, hệ thống dùng mặc định (logo chữ, không favicon).
 */
@Entity
@Table(name = "site_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "setting_key", nullable = false, unique = true)
    String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    String settingValue;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
