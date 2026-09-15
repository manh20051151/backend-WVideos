package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.NavItemCreateRequest;
import com.example.backendWVideos.dto.request.NavItemUpdateRequest;
import com.example.backendWVideos.dto.response.NavItemResponse;
import com.example.backendWVideos.service.NavItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nav-items")
@RequiredArgsConstructor
public class NavItemController {
    
    private final NavItemService navItemService;
    
    /**
     * Lấy tất cả mục menu đang hoạt động (public - cho frontend)
     */
    @GetMapping
    public ResponseEntity<List<NavItemResponse>> getActiveNavItems() {
        List<NavItemResponse> navItems = navItemService.getActiveNavItems();
        return ResponseEntity.ok(navItems);
    }
    
    /**
     * Lấy tất cả mục menu (admin only) với phân trang và tìm kiếm
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<NavItemResponse>> getAllNavItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<NavItemResponse> navItems = navItemService.getAllNavItems(pageable, search);
        return ResponseEntity.ok(navItems);
    }
    
    /**
     * Lấy mục menu theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NavItemResponse> getNavItemById(@PathVariable String id) {
        NavItemResponse navItem = navItemService.getNavItemById(id);
        return ResponseEntity.ok(navItem);
    }
    
    /**
     * Tạo mục menu mới (admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NavItemResponse> createNavItem(
            @Valid @RequestBody NavItemCreateRequest request) {
        NavItemResponse navItem = navItemService.createNavItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(navItem);
    }
    
    /**
     * Cập nhật mục menu (admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NavItemResponse> updateNavItem(
            @PathVariable String id,
            @Valid @RequestBody NavItemUpdateRequest request) {
        NavItemResponse navItem = navItemService.updateNavItem(id, request);
        return ResponseEntity.ok(navItem);
    }
    
    /**
     * Xóa mục menu (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNavItem(@PathVariable String id) {
        navItemService.deleteNavItem(id);
        return ResponseEntity.noContent().build();
    }
}
