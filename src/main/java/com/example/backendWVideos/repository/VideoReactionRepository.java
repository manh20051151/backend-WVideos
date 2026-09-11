package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.VideoReaction;
import com.example.backendWVideos.enums.VideoReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoReactionRepository extends JpaRepository<VideoReaction, String> {

    Optional<VideoReaction> findByUserIdAndVideoId(String userId, String videoId);

    boolean existsByUserIdAndVideoIdAndReactionType(String userId, String videoId, VideoReactionType reactionType);

    void deleteByUserIdAndVideoId(String userId, String videoId);

    long countByVideoIdAndReactionType(String videoId, VideoReactionType reactionType);

    Page<VideoReaction> findByUserIdAndReactionTypeOrderByCreatedAtDesc(String userId, VideoReactionType reactionType, Pageable pageable);
}