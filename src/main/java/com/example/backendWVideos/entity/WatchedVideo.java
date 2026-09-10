package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "watched_videos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_watched_user_video",
                columnNames = {"user_id", "video_id"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchedVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "video_id", nullable = false, length = 50)
    private String videoId;

    @Column(name = "watched_at", nullable = false)
    private LocalDateTime watchedAt;
}
