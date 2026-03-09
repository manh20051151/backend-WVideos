package com.example.backendWVideos.entity;

import com.example.backendWVideos.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "videos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // DoodStream file code
    @Column(name = "file_code", unique = true)
    private String fileCode;
    
    // URLs từ DoodStream
    @Column(name = "download_url")
    private String downloadUrl;
    
    @Column(name = "embed_url")
    private String embedUrl;
    
    @Column(name = "protected_embed_url")
    private String protectedEmbedUrl;
    
    @Column(name = "protected_download_url")
    private String protectedDownloadUrl;
    
    // Thumbnails
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
    
    @Column(name = "splash_image_url")
    private String splashImageUrl;
    
    // Video metadata
    @Column(name = "file_size")
    private Long fileSize; // bytes
    
    @Column(name = "duration")
    private Long duration; // seconds
    
    @Column(name = "views")
    private Long views = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status = VideoStatus.UPLOADING;
    
    // User relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "is_public")
    private Boolean isPublic = true;
    
    // Thể loại video (nhiều thể loại)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "video_categories",
        joinColumns = @JoinColumn(name = "video_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "uploaded_to_doodstream_at")
    private LocalDateTime uploadedToDoodStreamAt;
    
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
