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
}
