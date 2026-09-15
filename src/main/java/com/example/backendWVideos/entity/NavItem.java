package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "nav_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String label; // Tên hiển thị trên menu (VD: "Tin tức", "Shorts")
    
    @Column(name = "slug", nullable = false, unique = true)
    private String slug; // URL-friendly identifier (VD: "tin-tuc", "shorts")
    
    @Column(name = "href", nullable = false)
    private String href; // Đường dẫn liên kết (VD: "/news", "/shorts")
    
    @Column(name = "icon")
    private String icon; // Icon đại diện (emoji hoặc icon class)
    
    @Column(name = "is_active")
    private Boolean isActive = true; // Có hiển thị trên menu không
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0; // Thứ tự sắp xếp trên menu
    
    @Column(name = "open_new_tab")
    private Boolean openNewTab = false; // Mở liên kết trong tab mới
    
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
