package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    
    // Kiểm tra đã đăng ký chưa
    boolean existsBySubscriberIdAndChannelId(String subscriberId, String channelId);
    
    // Xóa đăng ký
    void deleteBySubscriberIdAndChannelId(String subscriberId, String channelId);
    
    // Đếm số người đăng ký kênh
    long countByChannelId(String channelId);
    
    // Đếm số kênh đã đăng ký
    long countBySubscriberId(String subscriberId);
    
    // Lấy danh sách người đăng ký theo kênh
    List<Subscription> findByChannelId(String channelId);
    
    // Lấy danh sách kênh đã đăng ký
    List<Subscription> findBySubscriberId(String subscriberId);
    
    // Trả về List thay vì Optional để tránh lỗi NonUniqueResult khi tồn tại dữ liệu trùng lặp
    List<Subscription> findBySubscriberIdAndChannelId(String subscriberId, String channelId);

    // Dọn dẹp bản ghi đăng ký trùng lặp: giữ lại 1 bản ghi (id nhỏ nhất) cho mỗi cặp (subscriber_id, channel_id)
    @Modifying
    @Query(value = "DELETE FROM subscriptions WHERE id NOT IN (" +
            "SELECT min_id FROM (SELECT MIN(id) AS min_id FROM subscriptions GROUP BY subscriber_id, channel_id) AS t)",
            nativeQuery = true)
    void deleteDuplicateSubscriptions();
}