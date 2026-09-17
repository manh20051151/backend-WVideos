package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.CommentReaction;
import com.example.backendWVideos.enums.CommentReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, String> {

    Optional<CommentReaction> findByUserIdAndCommentId(String userId, String commentId);

    List<CommentReaction> findByUserIdAndCommentIdIn(String userId, Collection<String> commentIds);

    long countByCommentIdAndReactionType(String commentId, CommentReactionType reactionType);

    default Map<String, CommentReactionType> getReactionsFor(String userId, Collection<String> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }
        return findByUserIdAndCommentIdIn(userId, commentIds).stream()
                .collect(Collectors.toMap(
                        r -> r.getComment().getId(),
                        CommentReaction::getReactionType,
                        (a, b) -> a));
    }
}
