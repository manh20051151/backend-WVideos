package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.ChannelSearchResult;
import com.example.backendWVideos.dto.response.NewsCategoryResponse;
import com.example.backendWVideos.dto.response.NewsResponse;
import com.example.backendWVideos.dto.response.SearchHistoryResponse;
import com.example.backendWVideos.dto.response.SearchSuggestResponse;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.entity.News;
import com.example.backendWVideos.entity.NewsCategory;
import com.example.backendWVideos.entity.SearchHistory;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.enums.NewsStatus;
import com.example.backendWVideos.mapper.VideoMapper;
import com.example.backendWVideos.repository.NewsRepository;
import com.example.backendWVideos.repository.SearchHistoryRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final VideoMapper videoMapper;
    private final SearchHistoryRepository searchHistoryRepository;

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

    // ==================== Lịch sử tìm kiếm (giống YouTube) ====================

    /**
     * Lưu từ khóa vào lịch sử (upsert): từ khóa đã tìm trước đó thì cập nhật
     * thời gian và tăng số lần tìm thay vì tạo dòng mới.
     */
    @Transactional
    public SearchHistoryResponse saveHistory(String q) {
        String userId = currentUserId();
        String keyword = normalize(q);
        if (userId == null || keyword == null) {
            return null; // Khách chưa đăng nhập: frontend tự lưu localStorage
        }
        LocalDateTime now = LocalDateTime.now();
        SearchHistory entry = searchHistoryRepository.findByUserIdAndQuery(userId, keyword)
                .orElse(null);
        if (entry == null) {
            entry = SearchHistory.builder()
                    .userId(userId)
                    .query(keyword)
                    .searchCount(1L)
                    .searchedAt(now)
                    .build();
        } else {
            entry.setSearchCount(entry.getSearchCount() + 1);
            entry.setSearchedAt(now);
        }
        SearchHistory saved = searchHistoryRepository.save(entry);
        log.info("✅ Đã lưu lịch sử tìm kiếm: user={}, từ khóa={}", userId, keyword);
        return toHistoryResponse(saved);
    }

    /**
     * Lịch sử tìm kiếm gần đây của user hiện tại (cho gợi ý khi focus ô tìm kiếm).
     */
    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getHistory(int limit) {
        String userId = currentUserId();
        if (userId == null) {
            return List.of();
        }
        int size = Math.min(Math.max(limit, 1), 20);
        return searchHistoryRepository.findTop20ByUserIdOrderBySearchedAtDesc(userId)
                .stream()
                .limit(size)
                .map(this::toHistoryResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Xóa 1 mục lịch sử (chỉ xóa được của chính mình).
     */
    @Transactional
    public boolean deleteHistoryItem(Long id) {
        String userId = currentUserId();
        if (userId == null || id == null) {
            return false;
        }
        return searchHistoryRepository.deleteByIdAndUserId(id, userId) > 0;
    }

    /**
     * Xóa toàn bộ lịch sử tìm kiếm của user hiện tại.
     */
    @Transactional
    public int clearHistory() {
        String userId = currentUserId();
        if (userId == null) {
            return 0;
        }
        int deleted = searchHistoryRepository.deleteAllByUserId(userId);
        log.info("🗑️ Đã xóa {} mục lịch sử tìm kiếm của user {}", deleted, userId);
        return deleted;
    }

    // Lấy userId của user đã đăng nhập (null nếu là khách)
    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return null;
        }
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    private SearchHistoryResponse toHistoryResponse(SearchHistory entry) {
        return SearchHistoryResponse.builder()
                .id(entry.getId())
                .query(entry.getQuery())
                .searchCount(entry.getSearchCount())
                .searchedAt(entry.getSearchedAt())
                .build();
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
