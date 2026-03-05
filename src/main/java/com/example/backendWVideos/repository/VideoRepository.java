package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    
    Optional<Video> findByFileCode(String fileCode);
    
    Page<Video> findByUserId(String userId, Pageable pageable);
    
    Page<Video> findByStatusAndIsPublic(VideoStatus status, Boolean isPublic, Pageable pageable);
    
    List<Video> findByUserIdAndStatus(String userId, VideoStatus status);
    
    Long countByUserId(String userId);
    
    Long countByUserIdAndStatus(String userId, VideoStatus status);
}
