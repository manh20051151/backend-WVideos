package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.ChannelSearchResult;
import com.example.backendWVideos.dto.response.NewsCategoryResponse;
import com.example.backendWVideos.dto.response.NewsResponse;
import com.example.backendWVideos.dto.response.SearchSuggestResponse;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.entity.News;
import com.example.backendWVideos.entity.NewsCategory;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.enums.NewsStatus;
import com.example.backendWVideos.mapper.VideoMapper;
import com.example.backendWVideos.repository.NewsRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tìm kiếm thông minh: gộp kết quả video + kênh + tin tức,
 * dùng cho ô tìm kiếm nhanh trên header và trang kết quả tìm kiếm.
 */
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final VideoMapper videoMapper;

    /**
     * Gợi ý nhanh khi user gõ trên header: top video + top kênh + top tin tức.
     */
    @Transactional(readOnly = true)
    public SearchSuggestResponse suggest(String q, int videoSize, int channelSize, int newsSize) {
        String keyword = normalize(q);
        if (keyword == null) {
            return SearchSuggestResponse.builder()
                    .videos(List.of())
                    .channels(List.of())
                    .news(List.of())
                    .build();
        }
        log.info("🔍 Gợi ý tìm kiếm: '{}'", keyword);

        Page<VideoResponse> videos = searchVideos(keyword, PageRequest.of(0, videoSize));
        Page<ChannelSearchResult> channels = searchChannels(keyword, PageRequest.of(0, channelSize));
        Page<NewsResponse> news = searchNews(keyword, PageRequest.of(0, newsSize));

        return SearchSuggestResponse.builder()
                .videos(videos.getContent())
                .channels(channels.getContent())
                .news(news.getContent())
                .build();
    }

    /**
     * Tìm video theo tiêu đề, mô tả hoặc tag (status READY, ưu tiên view cao).
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> searchVideos(String q, Pageable pageable) {
        String keyword = normalize(q);
        if (keyword == null) {
            return Page.empty(pageable);
        }
        return videoRepository.searchByKeyword(keyword, pageable)
                .map(videoMapper::toVideoResponse);
    }

    /**
     * Tìm kênh theo tên hiển thị hoặc slug kênh (user bị khóa tự bị loại bởi @SQLRestriction).
     */
    @Transactional(readOnly = true)
    public Page<ChannelSearchResult> searchChannels(String q, Pageable pageable) {
        String keyword = normalize(q);
        if (keyword == null) {
            return Page.empty(pageable);
        }
        return userRepository.searchChannelsByKeyword(keyword, pageable)
                .map(this::toChannelResult);
    }

    /**
     * Tìm tin tức ĐÃ XUẤT BẢN theo tiêu đề hoặc tóm tắt.
     */
    @Transactional(readOnly = true)
    public Page<NewsResponse> searchNews(String q, Pageable pageable) {
        String keyword = normalize(q);
        if (keyword == null) {
            return Page.empty(pageable);
        }
        return newsRepository.searchPublished(NewsStatus.PUBLISHED, keyword, pageable)
                .map(this::mapNews);
    }

    private String normalize(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ChannelSearchResult toChannelResult(User user) {
        return ChannelSearchResult.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .channelSlug(user.getChannelSlug())
                .build();
    }

    // Map nhẹ cho tìm kiếm: không trả content đầy đủ để giảm payload
    private NewsResponse mapNews(News news) {
        NewsCategoryResponse categoryResp = null;
        if (news.getCategory() != null) {
            NewsCategory c = news.getCategory();
            categoryResp = NewsCategoryResponse.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .slug(c.getSlug())
                    .isActive(c.getIsActive())
                    .sortOrder(c.getSortOrder())
                    .build();
        }
        return NewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .slug(news.getSlug())
                .summary(news.getSummary())
                .thumbnailUrl(news.getThumbnailUrl())
                .status(news.getStatus())
                .category(categoryResp)
                .authorName(news.getAuthorName())
                .views(news.getViews())
                .createdAt(news.getCreatedAt())
                .publishedAt(news.getPublishedAt())
                .build();
    }
}
