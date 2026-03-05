package com.example.backendWVideos.mapper;

import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.entity.Video;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "video", target = "username", qualifiedByName = "getUsername")
    VideoResponse toVideoResponse(Video video);
    
    @Named("getUsername")
    default String getUsername(Video video) {
        if (video.getUser() == null) {
            return null;
        }
        // Nếu username null, dùng email làm fallback
        String username = video.getUser().getUsername();
        return username != null ? username : video.getUser().getEmail();
    }
}
