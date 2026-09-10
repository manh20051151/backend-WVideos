package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.WatchedVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchedVideoRepository extends JpaRepository<WatchedVideo, Long> {

    Optional<WatchedVideo> findByUserIdAndVideoId(String userId, String videoId);

    boolean existsByUserIdAndVideoId(String userId, String videoId);

    List<WatchedVideo> findByUserId(String userId);

    @Modifying
    @Query("DELETE FROM WatchedVideo w WHERE w.watchedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
