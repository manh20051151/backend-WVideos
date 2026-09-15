package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Notification;
import com.example.backendWVideos.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(String recipientId);

    void deleteByRecipientIdAndActorId(String recipientId, String actorId);

    // Trả về List thay vì Optional để tránh lỗi NonUniqueResult khi tồn tại thông báo trùng lặp
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.type = :type AND n.relatedId = :relatedId")
    List<Notification> findByRecipientIdAndTypeAndRelatedId(
            @Param("userId") String userId,
            @Param("type") NotificationType type,
            @Param("relatedId") String relatedId
    );

    // Dọn dẹp thông báo trùng lặp: giữ lại 1 bản ghi (id nhỏ nhất) cho mỗi cặp (recipient_id, type, related_id)
    @Modifying
    @Query(value = "DELETE FROM notifications WHERE id NOT IN (" +
            "SELECT min_id FROM (SELECT MIN(id) AS min_id FROM notifications GROUP BY recipient_id, type, related_id) AS t)",
            nativeQuery = true)
    void deleteDuplicateNotifications();
}
