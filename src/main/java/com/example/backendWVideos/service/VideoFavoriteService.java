package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.FavoriteResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.entity.VideoFavorite;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoFavoriteRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoFavoriteService {

    private final VideoFavoriteRepository videoFavoriteRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Transactional
    public FavoriteResponse toggleFavorite(String userEmail, String videoId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        
        var existingFavorite = videoFavoriteRepository.findByUserIdAndVideoId(user.getId(), videoId);
        
        if (existingFavorite.isPresent()) {
            videoFavoriteRepository.delete(existingFavorite.get());
            video.setFavoritesCount(Math.max(0, video.getFavoritesCount() - 1));
            videoRepository.save(video);
            log.info("User {} un-favorited video {}", userEmail, videoId);
            return null;
        } else {
            VideoFavorite favorite = VideoFavorite.builder()
                    .user(user)
                    .video(video)
                    .build();
            videoFavoriteRepository.save(favorite);
            video.setFavoritesCount(video.getFavoritesCount() + 1);
            videoRepository.save(video);
            log.info("User {} favorited video {}", userEmail, videoId);
            
            return FavoriteResponse.builder()
                    .id(favorite.getId())
                    .userId(user.getId())
                    .username(user.getUsername())
                    .videoId(videoId)
                    .createdAt(favorite.getCreatedAt())
                    .build();
        }
    }

    public boolean isFavorited(String userEmail, String videoId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return videoFavoriteRepository.existsByUserIdAndVideoId(user.getId(), videoId);
    }

    public List<FavoriteResponse> getUserFavorites(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        return videoFavoriteRepository.findAll().stream()
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .map(f -> FavoriteResponse.builder()
                        .id(f.getId())
                        .userId(user.getId())
                        .username(user.getUsername())
                        .videoId(f.getVideo().getId())
                        .createdAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
