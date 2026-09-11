package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.SubscriptionRequest;
import com.example.backendWVideos.dto.response.SubscriptionResponse;
import com.example.backendWVideos.dto.response.UserResponse;
import com.example.backendWVideos.entity.Subscription;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.mapper.UserMapper;
import com.example.backendWVideos.repository.SubscriptionRepository;
import com.example.backendWVideos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    // Đăng ký kênh
    @Transactional
    public ApiResponse<SubscriptionResponse> subscribe(SubscriptionRequest request) {
        User subscriber = getCurrentUser();
        User channel = userRepository.findById(request.getChannelId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Không thể tự đăng ký kênh của mình
        if (subscriber.getId().equals(channel.getId())) {
            throw new AppException(ErrorCode.INVALID_DATA);
        }
        
        // Kiểm tra đã đăng ký chưa
        if (subscriptionRepository.existsBySubscriberIdAndChannelId(subscriber.getId(), channel.getId())) {
            throw new AppException(ErrorCode.ALREADY_SUBSCRIBED);
        }
        
        Subscription subscription = Subscription.builder()
                .subscriber(subscriber)
                .channel(channel)
                .build();
        
        subscriptionRepository.save(subscription);
        
        long subscriberCount = subscriptionRepository.countByChannelId(channel.getId());
        
        log.info("User {} subscribed to channel {}", subscriber.getEmail(), channel.getEmail());
        
        return ApiResponse.<SubscriptionResponse>builder()
                .code(1000)
                .message("Đăng ký thành công")
                .result(SubscriptionResponse.builder()
                        .id(subscription.getId())
                        .subscriberId(subscriber.getId())
                        .channelId(channel.getId())
                        .subscriberCount(subscriberCount)
                        .subscribed(true)
                        .build())
                .build();
    }
    
    // Hủy đăng ký kênh
    @Transactional
    public ApiResponse<SubscriptionResponse> unsubscribe(SubscriptionRequest request) {
        User subscriber = getCurrentUser();
        User channel = userRepository.findById(request.getChannelId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Subscription subscription = subscriptionRepository.findBySubscriberIdAndChannelId(
                subscriber.getId(), channel.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_SUBSCRIBED));
        
        subscriptionRepository.delete(subscription);
        
        long subscriberCount = subscriptionRepository.countByChannelId(channel.getId());
        
        log.info("User {} unsubscribed from channel {}", subscriber.getEmail(), channel.getEmail());
        
        return ApiResponse.<SubscriptionResponse>builder()
                .code(1000)
                .message("Hủy đăng ký thành công")
                .result(SubscriptionResponse.builder()
                        .channelId(channel.getId())
                        .subscriberCount(subscriberCount)
                        .subscribed(false)
                        .build())
                .build();
    }
    
    // Lấy số người đăng ký của kênh
    public long getSubscriberCount(String channelId) {
        return subscriptionRepository.countByChannelId(channelId);
    }
    
    // Kiểm tra đã đăng ký chưa
    public boolean isSubscribed(String subscriberId, String channelId) {
        return subscriptionRepository.existsBySubscriberIdAndChannelId(subscriberId, channelId);
    }
    
    // Lấy danh sách người đăng ký
    public List<Subscription> getSubscribers(String channelId) {
        return subscriptionRepository.findByChannelId(channelId);
    }
    
    // Lấy danh sách kênh đã đăng ký
    public List<Subscription> getSubscriptions(String subscriberId) {
        return subscriptionRepository.findBySubscriberId(subscriberId);
    }

    // Lấy danh sách kênh đã đăng ký (thông tin chi tiết)
    @Transactional(readOnly = true)
    public List<UserResponse> getMyChannels() {
        User user = getCurrentUser();
        return subscriptionRepository.findBySubscriberId(user.getId()).stream()
                .map(s -> {
                    User channel = s.getChannel();
                    Hibernate.initialize(channel);
                    return userMapper.toUserResponse(channel);
                })
                .toList();
    }
    
    // Lấy số kênh đã đăng ký
    public long getSubscriptionCount(String subscriberId) {
        return subscriptionRepository.countBySubscriberId(subscriberId);
    }
    
    // Lấy user hiện tại (public cho controller)
    public User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}