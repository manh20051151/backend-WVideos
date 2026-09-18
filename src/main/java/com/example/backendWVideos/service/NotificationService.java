package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.NotificationResponse;
import com.example.backendWVideos.entity.Notification;
import com.example.backendWVideos.entity.Subscription;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.NotificationType;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.NotificationRepository;
import com.example.backendWVideos.repository.SubscriptionRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VideoRepository videoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String USER_DESTINATION = "/queue/notifications";

    private String videoThumbnail(Video video) {
        if (video == null) return null;
        return video.getThumbnailUrl() != null ? video.getThumbnailUrl() : video.getSplashImageUrl();
    }

    /**
     * Tạo và lưu notification, đồng thời push realtime qua WebSocket tới người nhận.
     * Dùng REQUIRES_NEW để lỗi lưu notification không làm rollback nghiệp vụ chính (VD: khóa bình luận).
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public NotificationResponse create(
            NotificationType type,
            String recipientId,
            String title,
            String content,
            String relatedId,
            String actorId,
            String actorName,
            String thumbnailUrl,
            String avatarUrl
    ) {
        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            log.warn("Không tìm thấy recipient {} để gửi notification", recipientId);
            return null;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .content(content)
                .read(false)
                .relatedId(relatedId)
                .actorId(actorId)
                .actorName(actorName)
                .avatarUrl(avatarUrl)
                .thumbnailUrl(thumbnailUrl)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse dto = toResponse(saved);

        try {
            messagingTemplate.convertAndSendToUser(recipientId, USER_DESTINATION, dto);
            log.info("📨 Đã push notification realtime tới user {}", recipientId);
        } catch (Exception e) {
            log.warn("Không gửi được notification realtime cho user {}: {}", recipientId, e.getMessage());
        }

        return dto;
    }

    // ==================== TRIGGERS ====================

    public void notifyNewComment(String videoOwnerId, String actorId, String actorName,
                                 String videoId, String videoTitle, String thumbnailUrl, String avatarUrl) {
        if (videoOwnerId.equals(actorId)) return;
        create(NotificationType.COMMENT, videoOwnerId,
                "Bình luận mới",
                actorName + " đã bình luận về video \"" + videoTitle + "\"",
                videoId, actorId, actorName, thumbnailUrl, avatarUrl);
    }

    public void notifyNewCommentReply(String parentCommentOwnerId, String actorId, String actorName,
                                      String videoId, String videoTitle, String replySnippet,
                                      String thumbnailUrl, String avatarUrl) {
        if (parentCommentOwnerId.equals(actorId)) return;
        String snippet = replySnippet != null ? replySnippet : "";
        create(NotificationType.COMMENT, parentCommentOwnerId,
                "Phản hồi bình luận",
                actorName + " đã trả lời bình luận của bạn trong video \"" + videoTitle + "\": " + snippet,
                videoId, actorId, actorName, thumbnailUrl, avatarUrl);
    }

    public void notifyNewSubscriber(String channelOwnerId, String subscriberId, String subscriberName, String avatarUrl) {
        if (channelOwnerId.equals(subscriberId)) return;
        // Tránh spam: bỏ qua nếu subscriber này từng đăng ký kênh trước đó
        // (hủy rồi đăng ký lại không sinh thông báo mới).
        boolean alreadyNotified = !notificationRepository
                .findByRecipientIdAndTypeAndRelatedId(channelOwnerId, NotificationType.SUBSCRIBE, subscriberId)
                .isEmpty();
        if (alreadyNotified) return;
        create(NotificationType.SUBSCRIBE, channelOwnerId,
                "Người đăng ký mới",
                subscriberName + " đã đăng ký kênh của bạn",
                subscriberId, subscriberId, subscriberName, null, avatarUrl);
    }

    public void notifyVideoPurchased(String videoOwnerId, String buyerId, String buyerName,
                                     String videoId, String videoTitle, Double amount, String thumbnailUrl, String avatarUrl) {
        if (videoOwnerId.equals(buyerId)) return;
        String money = amount != null ? String.format("%,.0f VNĐ", amount) : "";
        create(NotificationType.PURCHASE, videoOwnerId,
                "Video được mua",
                buyerName + " đã mua video \"" + videoTitle + "\" (" + money + ")",
                videoId, buyerId, buyerName, thumbnailUrl, avatarUrl);
    }

    public void notifyNewLike(String videoOwnerId, String actorId, String actorName,
                             String videoId, String videoTitle, String thumbnailUrl, String avatarUrl) {
        if (videoOwnerId.equals(actorId)) return;
        create(NotificationType.LIKE, videoOwnerId,
                "Lượt thích mới",
                actorName + " đã thích video \"" + videoTitle + "\"",
                videoId, actorId, actorName, thumbnailUrl, avatarUrl);
    }

    /**
     * Thông báo cho tất cả người đăng ký khi kênh có video mới (status READY).
     */
    public void notifyNewVideoToSubscribers(String channelOwnerId, String videoId, String videoTitle, String thumbnailUrl, String avatarUrl) {
        User channelOwner = userRepository.findById(channelOwnerId).orElse(null);
        if (channelOwner == null) return;
        String channelName = channelOwner.getFullName() != null ? channelOwner.getFullName() : "Kênh của bạn";

        List<Subscription> subscribers = subscriptionRepository.findByChannelId(channelOwnerId);
        for (Subscription sub : subscribers) {
            String subscriberId = sub.getSubscriber().getId();
            if (subscriberId.equals(channelOwnerId)) continue;
            if (sub.isMuted()) continue;
            create(NotificationType.NEW_VIDEO, subscriberId,
                    "Video mới",
                    channelName + " vừa đăng video mới: \"" + videoTitle + "\"",
                    videoId, channelOwnerId, channelName, thumbnailUrl, avatarUrl);
        }
    }

    /**
     * Quản trị viên gửi thông báo.
     * - Nếu có recipientId/recipientEmail: gửi cho một người dùng cụ thể.
     * - Ngược lại: broadcast cho tất cả người dùng (trừ chính admin).
     * Trả về số lượng người dùng đã nhận thông báo.
     */
    public long adminSend(String adminId, String adminName, String adminAvatar,
                         String recipientId, String recipientEmail,
                         String title, String content) {
        if (recipientId != null || recipientEmail != null) {
            User recipient = resolveRecipient(recipientId, recipientEmail);
            if (recipient == null) {
                throw new AppException(ErrorCode.USER_NOT_EXISTED);
            }
            create(NotificationType.ANNOUNCEMENT, recipient.getId(),
                    title, content, null, adminId, adminName, null, adminAvatar);
            return 1;
        }

        List<User> users = userRepository.findAll();
        long count = 0;
        for (User u : users) {
            if (u.getId().equals(adminId)) continue;
            create(NotificationType.ANNOUNCEMENT, u.getId(),
                    title, content, null, adminId, adminName, null, adminAvatar);
            count++;
        }
        return count;
    }

    private User resolveRecipient(String recipientId, String recipientEmail) {
        if (recipientId != null) {
            return userRepository.findById(recipientId).orElse(null);
        }
        if (recipientEmail != null) {
            return userRepository.findByEmail(recipientEmail).orElse(null);
        }
        return null;
    }

    // ==================== REST QUERIES ====================

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> list = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .getContent();
        list.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(list);
    }

    @Transactional
    public void delete(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllFromActor(String userId, String actorId) {
        notificationRepository.deleteByRecipientIdAndActorId(userId, actorId);
    }

    private NotificationResponse toResponse(Notification n) {
        // Backfill ảnh cho thông báo cũ chưa có thumbnail/avatar
        String thumbnailUrl = n.getThumbnailUrl();
        String videoSlug = null;

        if (n.getRelatedId() != null && n.getType() != NotificationType.SUBSCRIBE) {
            // relatedId của các loại video là videoId
            java.util.Optional<Video> videoOpt = videoRepository.findById(n.getRelatedId());
            if (videoOpt.isPresent()) {
                Video video = videoOpt.get();
                if (thumbnailUrl == null) {
                    thumbnailUrl = video.getThumbnailUrl() != null ? video.getThumbnailUrl() : video.getSplashImageUrl();
                }
                videoSlug = video.getSlug();
            }
        }

        // Luôn lấy avatar hiện tại của người thực hiện từ DB,
        // tránh hiển thị ảnh cũ (snapshot thời điểm tạo notification)
        String avatarUrl = currentActorAvatar(n);
        if (avatarUrl == null) {
            avatarUrl = n.getAvatarUrl();
        }

        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .read(n.isRead())
                .relatedId(n.getRelatedId())
                .videoSlug(videoSlug)
                .actorId(n.getActorId())
                .actorName(n.getActorName())
                .avatarUrl(avatarUrl)
                .thumbnailUrl(thumbnailUrl)
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String currentActorAvatar(Notification n) {
        if (n.getActorId() != null) {
            return userRepository.findById(n.getActorId())
                    .map(User::getAvatar).orElse(null);
        }
        if (n.getType() == NotificationType.SUBSCRIBE && n.getRelatedId() != null) {
            // SUBSCRIBE: relatedId chính là subscriberId
            return userRepository.findById(n.getRelatedId())
                    .map(User::getAvatar).orElse(null);
        }
        return null;
    }
}
