package com.example.backendWVideos.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionResponse {
    String id;
    String subscriberId;
    String channelId;
    Long subscriberCount;
    boolean subscribed;
    LocalDateTime subscribedAt;
}