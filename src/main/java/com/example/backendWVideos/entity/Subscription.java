package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"subscriber_id", "channel_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    User subscriber; // Người đăng ký

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    User channel; // Kênh được đăng ký

    @Column(name = "subscribed_at", nullable = false)
    LocalDateTime subscribedAt;
    
    @PrePersist
    protected void onCreate() {
        subscribedAt = LocalDateTime.now();
    }
}