package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
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
    
    // Kiểm tra đã đăng ký - trả về Optional
    Optional<Subscription> findBySubscriberIdAndChannelId(String subscriberId, String channelId);
}