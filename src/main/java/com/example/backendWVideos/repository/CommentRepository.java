package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    
    Page<Comment> findByVideoIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(String videoId, Pageable pageable);
    
    long countByVideoIdAndIsDeletedFalse(String videoId);
    
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(String parentId);
    
    // Moderation queries
    Page<Comment> findByVideoIdAndStatusAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
        String videoId, 
        com.example.backendWVideos.enums.CommentStatus status, 
        Pageable pageable
    );
    
    Page<Comment> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(
        com.example.backendWVideos.enums.CommentStatus status, 
        Pageable pageable
    );
    
    long countByStatusAndIsDeletedFalse(com.example.backendWVideos.enums.CommentStatus status);
    
    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId " +
           "AND c.parent IS NULL AND c.isDeleted = false " +
           "AND (c.status = :status OR (c.status = com.example.backendWVideos.enums.CommentStatus.PENDING AND c.user.email = :userEmail)) " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findByVideoIdWithUserPending(
        @Param("videoId") String videoId,
        @Param("status") com.example.backendWVideos.enums.CommentStatus status,
        @Param("userEmail") String userEmail,
        Pageable pageable
    );
}
