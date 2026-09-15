package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.VideoReaction;
import com.example.backendWVideos.enums.VideoReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoReactionRepository extends JpaRepository<VideoReaction, String> {

    // Trả về List thay vì Optional để tránh lỗi NonUniqueResult khi tồn tại dữ liệu trùng lặp (cùng user + video)
    List<VideoReaction> findByUserIdAndVideoId(String userId, String videoId);

    boolean existsByUserIdAndVideoIdAndReactionType(String userId, String videoId, VideoReactionType reactionType);

    void deleteByUserIdAndVideoId(String userId, String videoId);

    long countByVideoIdAndReactionType(String videoId, VideoReactionType reactionType);

    Page<VideoReaction> findByUserIdAndReactionTypeOrderByCreatedAtDesc(String userId, VideoReactionType reactionType, Pageable pageable);

    // Dọn dẹp bản ghi reaction trùng lặp: giữ lại 1 bản ghi (id nhỏ nhất) cho mỗi cặp (user_id, video_id)
    @Modifying
    @Query(value = "DELETE FROM video_reactions WHERE id NOT IN (" +
            "SELECT min_id FROM (SELECT MIN(id) AS min_id FROM video_reactions GROUP BY user_id, video_id) AS t)",
            nativeQuery = true)
    void deleteDuplicateReactions();
}