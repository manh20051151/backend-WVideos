package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_view_logs",
        indexes = {
                @Index(name = "idx_vvl_viewed_at", columnList = "viewed_at"),
                @Index(name = "idx_vvl_video_viewed", columnList = "video_id, viewed_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 50)
    private String videoId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "ip", length = 45)
    private String ip;
}
