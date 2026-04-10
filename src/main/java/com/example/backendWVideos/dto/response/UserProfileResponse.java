package com.example.backendWVideos.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    String id;
    String email;
    String fullName;
    String avatar;
    Long subscriberCount;
    Long videoCount;
    Long totalViews;
    Boolean isSubscribed;
    List<VideoResponse> videos;
}