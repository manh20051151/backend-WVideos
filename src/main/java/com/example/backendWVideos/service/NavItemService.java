package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.NavItemCreateRequest;
import com.example.backendWVideos.dto.request.NavItemUpdateRequest;
import com.example.backendWVideos.dto.response.NavItemResponse;
import com.example.backendWVideos.entity.NavItem;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.NavItemRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NavItemService {
    
    private final NavItemRepository navItemRepository;
    private final UserRepository userRepository;
    
    /**
     * Lấy tất cả nav item (cho admin) với phân trang và tìm kiếm
     */
    public Page<NavItemResponse> getAllNavItems(Pageable pageable, String search) {
        Page<NavItem> navItems;
        
        if (search != null && !search.trim().isEmpty()) {
            navItems = navItemRepository.findBySearchQuery(search.trim(), pageable);
        } else {
            navItems = navItemRepository.findAllWithCreatedBy(pageable);
        }
        
        return navItems.map(this::mapToResponse);
    }
    
    /**
     * Lấy tất cả nav item đang hoạt động (cho user/frontend)
     */
    public List<NavItemResponse> getActiveNavItems() {
        List<NavItem> navItems = navItemRepository.findAllActiveOrderBySortOrder();
        return navItems.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy nav item theo ID
     */
    public NavItemResponse getNavItemById(String id) {
        NavItem navItem = navItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NAV_ITEM_NOT_FOUND));
        return mapToResponse(navItem);
    }
    
    /**
     * Tạo nav item mới
     */
    @Transactional
    public NavItemResponse createNavItem(NavItemCreateRequest request) {
        if (navItemRepository.findByLabel(request.getLabel()).isPresent()) {
            throw new AppException(ErrorCode.NAV_ITEM_LABEL_EXISTED);
        }
        
        if (navItemRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new AppException(ErrorCode.NAV_ITEM_SLUG_EXISTED);
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            email = jwt.getSubject();
            log.info("Lấy email từ JWT: {}", email);
        }
        
        if (email == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        NavItem navItem = NavItem.builder()
                .label(request.getLabel())
                .slug(request.getSlug())
                .href(request.getHref())
                .icon(request.getIcon())
                .isActive(request.getIsActive())
                .openNewTab(request.getOpenNewTab())
                .sortOrder(request.getSortOrder())
                .createdBy(admin)
                .createdByName(admin.getFullName() != null ? admin.getFullName() : admin.getEmail())
                .build();
        
        NavItem savedNavItem = navItemRepository.save(navItem);
        log.info("Đã tạo mục menu mới: {} bởi admin: {}", savedNavItem.getLabel(), admin.getEmail());
        
        return mapToResponse(savedNavItem);
    }
    
    /**
     * Cập nhật nav item
     */
    @Transactional
    public NavItemResponse updateNavItem(String id, NavItemUpdateRequest request) {
        NavItem navItem = navItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NAV_ITEM_NOT_FOUND));
        
        if (navItemRepository.existsByLabelAndIdNot(request.getLabel(), id)) {
            throw new AppException(ErrorCode.NAV_ITEM_LABEL_EXISTED);
        }
        
        if (navItemRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new AppException(ErrorCode.NAV_ITEM_SLUG_EXISTED);
        }
        
        navItem.setLabel(request.getLabel());
        navItem.setSlug(request.getSlug());
        navItem.setHref(request.getHref());
        navItem.setIcon(request.getIcon());
        navItem.setIsActive(request.getIsActive());
        navItem.setOpenNewTab(request.getOpenNewTab());
        navItem.setSortOrder(request.getSortOrder());
        
        NavItem updatedNavItem = navItemRepository.save(navItem);
        log.info("Đã cập nhật mục menu: {}", updatedNavItem.getLabel());
        
        return mapToResponse(updatedNavItem);
    }
    
    /**
     * Xóa nav item
     */
    @Transactional
    public void deleteNavItem(String id) {
        NavItem navItem = navItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NAV_ITEM_NOT_FOUND));
        
        navItemRepository.delete(navItem);
        log.info("Đã xóa mục menu: {}", navItem.getLabel());
    }
    
    /**
     * Chuyển đổi Entity sang Response DTO
     */
    private NavItemResponse mapToResponse(NavItem navItem) {
        return NavItemResponse.builder()
                .id(navItem.getId())
                .label(navItem.getLabel())
                .slug(navItem.getSlug())
                .href(navItem.getHref())
                .icon(navItem.getIcon())
                .isActive(navItem.getIsActive())
                .openNewTab(navItem.getOpenNewTab())
                .sortOrder(navItem.getSortOrder())
                .createdAt(navItem.getCreatedAt())
                .updatedAt(navItem.getUpdatedAt())
                .createdByUsername(navItem.getCreatedBy() != null ? navItem.getCreatedBy().getFullName() : null)
                .createdByName(navItem.getCreatedByName())
                .build();
    }
}
