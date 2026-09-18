package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.FooterLinkCreateRequest;
import com.example.backendWVideos.dto.request.FooterLinkUpdateRequest;
import com.example.backendWVideos.dto.request.FooterSettingCreateRequest;
import com.example.backendWVideos.dto.request.FooterSettingUpdateRequest;
import com.example.backendWVideos.dto.response.FooterInfoResponse;
import com.example.backendWVideos.dto.response.FooterLinkResponse;
import com.example.backendWVideos.dto.response.FooterSettingResponse;
import com.example.backendWVideos.enums.FooterSection;
import com.example.backendWVideos.service.FooterService;
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
@RequestMapping("/footer")
@RequiredArgsConstructor
public class FooterController {

    private final FooterService footerService;

    /**
     * Lấy toàn bộ thông tin footer đang hoạt động (public - cho frontend)
     */
    @GetMapping
    public ResponseEntity<FooterInfoResponse> getFooterInfo() {
        FooterInfoResponse footerInfo = footerService.getFooterInfo();
        return ResponseEntity.ok(footerInfo);
    }

    /**
     * Lấy tất cả footer link (admin only) với phân trang, tìm kiếm và lọc theo section
     */
    @GetMapping("/links/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FooterLinkResponse>> getAllFooterLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FooterSection section) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<FooterLinkResponse> footerLinks = footerService.getAllFooterLinks(pageable, search, section);
        return ResponseEntity.ok(footerLinks);
    }

    /**
     * Lấy footer link theo ID
     */
    @GetMapping("/links/{id}")
    public ResponseEntity<FooterLinkResponse> getFooterLinkById(@PathVariable String id) {
        FooterLinkResponse footerLink = footerService.getFooterLinkById(id);
        return ResponseEntity.ok(footerLink);
    }

    /**
     * Tạo footer link mới (admin only)
     */
    @PostMapping("/links")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FooterLinkResponse> createFooterLink(
            @Valid @RequestBody FooterLinkCreateRequest request) {
        FooterLinkResponse footerLink = footerService.createFooterLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(footerLink);
    }

    /**
     * Cập nhật footer link (admin only)
     */
    @PutMapping("/links/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FooterLinkResponse> updateFooterLink(
            @PathVariable String id,
            @Valid @RequestBody FooterLinkUpdateRequest request) {
        FooterLinkResponse footerLink = footerService.updateFooterLink(id, request);
        return ResponseEntity.ok(footerLink);
    }

    /**
     * Xóa footer link (admin only)
     */
    @DeleteMapping("/links/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFooterLink(@PathVariable String id) {
        footerService.deleteFooterLink(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lấy tất cả cấu hình footer (admin only)
     */
    @GetMapping("/settings/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FooterSettingResponse>> getAllFooterSettings() {
        List<FooterSettingResponse> footerSettings = footerService.getAllFooterSettings();
        return ResponseEntity.ok(footerSettings);
    }

    /**
     * Tạo cấu hình footer mới (admin only)
     */
    @PostMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FooterSettingResponse> createFooterSetting(
            @Valid @RequestBody FooterSettingCreateRequest request) {
        FooterSettingResponse footerSetting = footerService.createFooterSetting(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(footerSetting);
    }

    /**
     * Cập nhật cấu hình footer (admin only)
     */
    @PutMapping("/settings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FooterSettingResponse> updateFooterSetting(
            @PathVariable String id,
            @Valid @RequestBody FooterSettingUpdateRequest request) {
        FooterSettingResponse footerSetting = footerService.updateFooterSetting(id, request);
        return ResponseEntity.ok(footerSetting);
    }

    /**
     * Xóa cấu hình footer (admin only)
     */
    @DeleteMapping("/settings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFooterSetting(@PathVariable String id) {
        footerService.deleteFooterSetting(id);
        return ResponseEntity.noContent().build();
    }
}