package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(String recipientId);

    void deleteByRecipientIdAndActorId(String recipientId, String actorId);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.type = :type AND n.relatedId = :relatedId")
    Optional<Notification> findByRecipientIdAndTypeAndRelatedId(
            @Param("userId") String userId,
            @Param("type") com.example.backendWVideos.enums.NotificationType type,
            @Param("relatedId") String relatedId
    );
}
