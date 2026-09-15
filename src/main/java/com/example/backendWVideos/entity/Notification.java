package com.example.backendWVideos.entity;

import com.example.backendWVideos.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean read = false;

    // ID liên quan: videoId, commentId, channelId,...
    @Column(name = "related_id")
    private String relatedId;

    // Người thực hiện hành động (nếu có)
    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_name")
    private String actorName;

    // Ảnh đại diện của người thực hiện (cho thông báo dạng user)
    @Column(name = "avatar_url")
    private String avatarUrl;

    // Ảnh thumbnail của video liên quan (cho thông báo dạng video)
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
