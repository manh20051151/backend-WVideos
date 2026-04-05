package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.VideoFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoFavoriteRepository extends JpaRepository<VideoFavorite, String> {
    
    Optional<VideoFavorite> findByUserIdAndVideoId(String userId, String videoId);
    
    boolean existsByUserIdAndVideoId(String userId, String videoId);
    
    void deleteByUserIdAndVideoId(String userId, String videoId);
    
    long countByVideoId(String videoId);
}
