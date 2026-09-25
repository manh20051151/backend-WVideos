package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.NewsCategoryRequest;
import com.example.backendWVideos.dto.response.NewsCategoryResponse;
import com.example.backendWVideos.entity.NewsCategory;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.NewsCategoryRepository;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCategoryService {

    private final NewsCategoryRepository newsCategoryRepository;
    private final UserRepository userRepository;
    private final CategoryTranslationService categoryTranslationService;

    public Page<NewsCategoryResponse> getAllCategories(Pageable pageable, String search) {
        Page<NewsCategory> categories = (search != null && !search.trim().isEmpty())
                ? newsCategoryRepository.findBySearchQuery(search.trim(), pageable)
                : newsCategoryRepository.findAllWithCreatedBy(pageable);
        return categories.map(this::mapToResponse);
    }

    /**
     * Lấy danh mục tin tức đang hoạt động.
     * Tên được bản địa hóa theo Accept-Language (fallback tiếng Việt nếu chưa có bản dịch).
     */
    public List<NewsCategoryResponse> getActiveCategories(Locale locale) {
        List<NewsCategory> categories = newsCategoryRepository.findAllActiveOrderBySortOrder();

        List<String> ids = categories.stream().map(NewsCategory::getId).toList();
        Map<String, String> localizedNames = categoryTranslationService
                .getLocalizedNames(CategoryTranslationService.OWNER_NEWS, ids, locale);

        return categories.stream()
                .map(c -> {
                    NewsCategoryResponse response = mapToResponse(c);
                    String translated = localizedNames.get(c.getId());
                    if (translated != null && !translated.isBlank()) {
                        response.setName(translated);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    public NewsCategoryResponse getCategoryById(String id) {
        NewsCategory category = newsCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_CATEGORY_NOT_FOUND));
        return mapToResponse(category);
    }

    @Transactional
    public NewsCategoryResponse createCategory(NewsCategoryRequest request) {
        if (newsCategoryRepository.findByName(request.getName()).isPresent()) {
            throw new AppException(ErrorCode.NEWS_CATEGORY_NAME_EXISTED);
        }
        if (newsCategoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new AppException(ErrorCode.NEWS_CATEGORY_SLUG_EXISTED);
        }

        User admin = getCurrentUser();

        NewsCategory category = NewsCategory.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .sortOrder(request.getSortOrder())
                .createdBy(admin)
                .createdByName(admin.getFullName() != null ? admin.getFullName() : admin.getEmail())
                .build();

        NewsCategory saved = newsCategoryRepository.save(category);
        log.info("Đã tạo danh mục tin tức: {} bởi admin: {}", saved.getName(), admin.getEmail());

        // Dịch tự động tên danh mục sang các ngôn ngữ sau khi commit
        categoryTranslationService.scheduleTranslate(
                CategoryTranslationService.OWNER_NEWS, saved.getId(), saved.getName());

        return mapToResponse(saved);
    }

    @Transactional
    public NewsCategoryResponse updateCategory(String id, NewsCategoryRequest request) {
        NewsCategory category = newsCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_CATEGORY_NOT_FOUND));

        if (newsCategoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new AppException(ErrorCode.NEWS_CATEGORY_NAME_EXISTED);
        }
        if (newsCategoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new AppException(ErrorCode.NEWS_CATEGORY_SLUG_EXISTED);
        }

        String oldName = category.getName();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive());
        category.setSortOrder(request.getSortOrder());

        NewsCategory updated = newsCategoryRepository.save(category);
        log.info("Đã cập nhật danh mục tin tức: {}", updated.getName());

        // Đổi tên -> xóa bản dịch cũ, dịch lại sau commit
        if (oldName != null && !oldName.equals(updated.getName())) {
            categoryTranslationService.scheduleRetranslate(
                    CategoryTranslationService.OWNER_NEWS, updated.getId(), updated.getName());
        }

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteCategory(String id) {
        NewsCategory category = newsCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NEWS_CATEGORY_NOT_FOUND));
        newsCategoryRepository.delete(category);
        categoryTranslationService.deleteTranslations(CategoryTranslationService.OWNER_NEWS, id);
        log.info("Đã xóa danh mục tin tức: {}", category.getName());
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

    private NewsCategoryResponse mapToResponse(NewsCategory category) {
        return NewsCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdByName(category.getCreatedByName())
                .build();
    }
}
