package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.CategoryCreateRequest;
import com.example.backendWVideos.dto.request.CategoryUpdateRequest;
import com.example.backendWVideos.dto.response.CategoryResponse;
import com.example.backendWVideos.entity.Category;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.CategoryRepository;
import com.example.backendWVideos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    
    /**
     * Lấy tất cả thể loại (cho admin) với phân trang và tìm kiếm
     */
    public Page<CategoryResponse> getAllCategories(Pageable pageable, String search) {
        Page<Category> categories;
        
        if (search != null && !search.trim().isEmpty()) {
            // Tìm kiếm theo tên, slug hoặc người tạo
            categories = categoryRepository.findBySearchQuery(search.trim(), pageable);
        } else {
            // Lấy tất cả
            categories = categoryRepository.findAll(pageable);
        }
        
        return categories.map(this::mapToResponse);
    }
    
    /**
     * Lấy tất cả thể loại đang hoạt động (cho user)
     */
    public List<CategoryResponse> getActiveCategories() {
        List<Category> categories = categoryRepository.findAllActiveOrderBySortOrder();
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy thể loại theo ID
     */
    public CategoryResponse getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return mapToResponse(category);
    }
    
    /**
     * Tạo thể loại mới
     */
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        // Kiểm tra tên đã tồn tại
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new AppException(ErrorCode.CATEGORY_NAME_EXISTED);
        }
        
        // Kiểm tra slug đã tồn tại
        if (categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTED);
        }
        
        // Lấy email từ JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getSubject(); // Email từ subject
            log.info("Lấy email từ JWT: {}", email);
        }
        
        if (email == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        // Tìm user bằng email
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Tạo category mới
        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .color(request.getColor())
                .icon(request.getIcon())
                .isActive(request.getIsActive())
                .sortOrder(request.getSortOrder())
                .createdBy(admin)
                .createdByName(admin.getFullName() != null ? admin.getFullName() : admin.getUsername())
                .build();
        
        Category savedCategory = categoryRepository.save(category);
        log.info("Đã tạo thể loại mới: {} bởi admin: {}", savedCategory.getName(), admin.getUsername());
        
        return mapToResponse(savedCategory);
    }
    
    /**
     * Cập nhật thể loại
     */
    @Transactional
    public CategoryResponse updateCategory(String id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        
        // Kiểm tra tên đã tồn tại (trừ category hiện tại)
        if (categoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new AppException(ErrorCode.CATEGORY_NAME_EXISTED);
        }
        
        // Kiểm tra slug đã tồn tại (trừ category hiện tại)
        if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTED);
        }
        
        // Cập nhật thông tin
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setIsActive(request.getIsActive());
        category.setSortOrder(request.getSortOrder());
        
        Category updatedCategory = categoryRepository.save(category);
        log.info("Đã cập nhật thể loại: {}", updatedCategory.getName());
        
        return mapToResponse(updatedCategory);
    }
    
    /**
     * Xóa thể loại
     */
    @Transactional
    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        
        // TODO: Kiểm tra xem có video nào đang sử dụng thể loại này không
        // Có thể set category = null cho các video hoặc không cho phép xóa
        
        categoryRepository.delete(category);
        log.info("Đã xóa thể loại: {}", category.getName());
    }
    
    /**
     * Chuyển đổi Entity sang Response DTO
     */
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .color(category.getColor())
                .icon(category.getIcon())
                .isActive(category.getIsActive())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdByUsername(category.getCreatedBy() != null ? category.getCreatedBy().getUsername() : null)
                .createdByName(category.getCreatedByName())
                .build();
    }
}