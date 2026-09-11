package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.request.SubscriptionRequest;
import com.example.backendWVideos.dto.response.SubscriptionResponse;
import com.example.backendWVideos.dto.response.UserResponse;
import com.example.backendWVideos.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    // Đăng ký kênh
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(@RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.subscribe(request));
    }
    
    // Hủy đăng ký kênh
    @DeleteMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> unsubscribe(@RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.unsubscribe(request));
    }
    
    // Lấy số người đăng ký của kênh
    @GetMapping("/count/{channelId}")
    public ResponseEntity<ApiResponse<Long>> getSubscriberCount(@PathVariable String channelId) {
        long count = subscriptionService.getSubscriberCount(channelId);
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(1000)
                .message("OK")
                .result(count)
                .build());
    }
    
    // Kiểm tra đã đăng ký chưa
    @GetMapping("/status/{channelId}")
    public ResponseEntity<ApiResponse<Boolean>> checkSubscription(@PathVariable String channelId) {
        boolean subscribed = subscriptionService.isSubscribed(
                subscriptionService.getCurrentUser().getId(), 
                channelId
        );
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .code(1000)
                .message("OK")
                .result(subscribed)
                .build());
    }
    
    // Lấy danh sách người đăng ký
    @GetMapping("/subscribers/{channelId}")
    public ResponseEntity<ApiResponse<List<String>>> getSubscribers(@PathVariable String channelId) {
        List<String> subscriberIds = subscriptionService.getSubscribers(channelId)
                .stream()
                .map(s -> s.getSubscriber().getId())
                .toList();
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .code(1000)
                .message("OK")
                .result(subscriberIds)
                .build());
    }
    
    // Lấy danh sách kênh đã đăng ký
    @GetMapping("/my-subscriptions")
    public ResponseEntity<ApiResponse<List<String>>> getMySubscriptions() {
        List<String> channelIds = subscriptionService.getSubscriptions(
                subscriptionService.getCurrentUser().getId()
        ).stream()
                .map(s -> s.getChannel().getId())
                .toList();
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .code(1000)
                .message("OK")
                .result(channelIds)
                .build());
    }

    // Lấy danh sách kênh đã đăng ký (thông tin chi tiết)
    @GetMapping("/my-channels")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getMyChannels() {
        List<UserResponse> channels = subscriptionService.getMyChannels();
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .code(1000)
                .message("OK")
                .result(channels)
                .build());
    }
}