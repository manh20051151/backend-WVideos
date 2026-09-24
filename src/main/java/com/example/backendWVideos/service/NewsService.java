package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.NewsRequest;
import com.example.backendWVideos.dto.response.NewsCategoryResponse;
import com.example.backendWVideos.dto.response.NewsResponse;
import com.example.backendWVideos.entity.News;
import com.example.backendWVideos.entity.NewsCategory;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.enums.NewsStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.NewsCategoryRepository;
import com.example.backendWVideos.repository.NewsRepository;
import com.example.backendWVideos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private final NewsRepository newsRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final UserRepository userRepository;

    public Page<NewsResponse> getAllNews(Pageable pageable, String search) {
        Page<News> news = (search != null && !search.trim().isEmpty())
                ? newsRepository.searchAll(search.trim(), pageable)
                : newsRepository.findAllWithRelations(pageable);
        return news.map(this::mapToResponse);
    }

    public Page<NewsResponse> getPublishedNews(Pageable pageable, String categoryId, String search) {
        NewsStatus status = NewsStatus.PUBLISHED;
        Page<News> news;
        if (search != null && !search.trim().isEmpty()) {
            news = newsRepository.searchPublished(status, search.trim(), pageable);
        } else if (categoryId != null && !categoryId.isBlank()) {
            news = newsRepository.findAllByStatusAndCategoryWithRelations(status, categoryId, pageable);
        } else {
            news = newsRepository.findAllByStatusWithRelations(status, pageable);
        }
        return news.map(this::mapToResponse);
    }

    public NewsResponse getNewsById(String id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_NOT_FOUND));
        return mapToResponse(news);
    }

    @Transactional
    public NewsResponse getPublishedNewsDetail(String idOrSlug) {
        // Hỗ trợ cả UUID lẫn slug trên URL (link ngoài dùng slug cho đẹp, link cũ theo id vẫn hoạt động)
        News news = newsRepository.findById(idOrSlug)
                .or(() -> newsRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_NOT_FOUND));
        if (news.getStatus() != NewsStatus.PUBLISHED) {
            throw new AppException(ErrorCode.NEWS_NOT_FOUND);
        }
        news.setViews(news.getViews() + 1);
        return mapToResponse(news);
    }

    @Transactional
    public NewsResponse createNews(NewsRequest request) {
        if (newsRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new AppException(ErrorCode.INVALID_DATA);
        }

        User author = getCurrentUser();
        NewsCategory category = resolveCategory(request.getCategoryId());

        News news = News.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .summary(request.getSummary())
                .content(request.getContent())
                .thumbnailUrl(request.getThumbnailUrl())
                .status(request.getStatus())
                .category(category)
                .author(author)
                .authorName(author.getFullName() != null ? author.getFullName() : author.getEmail())
                .publishedAt(request.getStatus() == NewsStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build();

        ensureSummary(news);

        News saved = newsRepository.save(news);
        log.info("Đã tạo tin tức: {} bởi {}", saved.getTitle(), author.getEmail());
        return mapToResponse(saved);
    }

    @Transactional
    public NewsResponse updateNews(String id, NewsRequest request) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_NOT_FOUND));
        if (newsRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new AppException(ErrorCode.INVALID_DATA);
        }

        NewsCategory category = resolveCategory(request.getCategoryId());

        news.setTitle(request.getTitle());
        news.setSlug(request.getSlug());
        news.setSummary(request.getSummary());
        news.setContent(request.getContent());
        news.setThumbnailUrl(request.getThumbnailUrl());
        news.setCategory(category);
        news.setStatus(request.getStatus());

        if (request.getStatus() == NewsStatus.PUBLISHED && news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }
        if (request.getStatus() == NewsStatus.DRAFT) {
            news.setPublishedAt(null);
        }

        ensureSummary(news);

        News updated = newsRepository.save(news);
        log.info("Đã cập nhật tin tức: {}", updated.getTitle());
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteNews(String id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_NOT_FOUND));
        newsRepository.delete(news);
        log.info("Đã xóa tin tức: {}", news.getTitle());
    }

    private NewsCategory resolveCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) return null;
        return newsCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_CATEGORY_NOT_FOUND));
    }

    private void ensureSummary(News news) {
        if (news.getSummary() == null || news.getSummary().isBlank()) {
            String text = news.getContent() != null ? HTML_TAG.matcher(news.getContent()).replaceAll("") : "";
            text = text.replaceAll("\\s+", " ").trim();
            if (text.length() > 300) text = text.substring(0, 300) + "...";
            news.setSummary(text);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            email = jwt.getSubject();
        }
        if (email == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private NewsResponse mapToResponse(News news) {
        NewsCategoryResponse categoryResp = null;
        if (news.getCategory() != null) {
            NewsCategory c = news.getCategory();
            categoryResp = NewsCategoryResponse.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .slug(c.getSlug())
                    .description(c.getDescription())
                    .isActive(c.getIsActive())
                    .sortOrder(c.getSortOrder())
                    .build();
        }
        return NewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .slug(news.getSlug())
                .summary(news.getSummary())
                .content(news.getContent())
                .thumbnailUrl(news.getThumbnailUrl())
                .status(news.getStatus())
                .category(categoryResp)
                .authorName(news.getAuthorName())
                .views(news.getViews())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .publishedAt(news.getPublishedAt())
                .build();
    }
}
