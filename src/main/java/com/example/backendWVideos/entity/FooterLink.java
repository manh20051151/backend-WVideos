package com.example.backendWVideos.entity;

import com.example.backendWVideos.enums.FooterSection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "footer_links")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FooterLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String label; // Tên hiển thị (VD: "Trang chủ", "Facebook")

    @Column(name = "href", nullable = false)
    private String href; // Đường dẫn liên kết (VD: "/news", "https://facebook.com/...")

    @Enumerated(EnumType.STRING)
    @Column(name = "section", nullable = false)
    private FooterSection section; // Khu vực hiển thị trong footer (QUICK_LINKS, SOCIAL...)

    @Column(name = "icon")
    private String icon; // Icon đại diện (emoji, icon class hoặc SVG path cho mạng xã hội)

    @Column(name = "is_active")
    private Boolean isActive = true; // Có hiển thị trên footer không

    @Column(name = "sort_order")
    private Integer sortOrder = 0; // Thứ tự sắp xếp trong section

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