package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String name; // Tên thể loại (VD: "Giải trí", "Giáo dục")
    
    @Column(name = "slug", nullable = false, unique = true)
    private String slug; // URL-friendly name (VD: "giai-tri", "giao-duc")
    
    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả thể loại
    
    @Column(name = "color")
    private String color; // Màu sắc đại diện (hex code)
    
    @Column(name = "icon")
    private String icon; // Icon đại diện (emoji hoặc icon class)
    
    @Column(name = "is_active")
    private Boolean isActive = true; // Có đang hoạt động không
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0; // Thứ tự sắp xếp
    
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