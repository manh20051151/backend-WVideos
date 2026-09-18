package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.FooterLinkCreateRequest;
import com.example.backendWVideos.dto.request.FooterLinkUpdateRequest;
import com.example.backendWVideos.dto.request.FooterSettingCreateRequest;
import com.example.backendWVideos.dto.request.FooterSettingUpdateRequest;
import com.example.backendWVideos.dto.response.FooterInfoResponse;
import com.example.backendWVideos.dto.response.FooterLinkResponse;
import com.example.backendWVideos.dto.response.FooterSettingResponse;
import com.example.backendWVideos.entity.FooterLink;
import com.example.backendWVideos.entity.FooterSetting;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.enums.FooterSection;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.FooterLinkRepository;
import com.example.backendWVideos.repository.FooterSettingRepository;
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FooterService {

    private final FooterLinkRepository footerLinkRepository;
    private final FooterSettingRepository footerSettingRepository;
    private final UserRepository userRepository;

    /**
     * Lấy toàn bộ thông tin footer đang hoạt động (cho frontend/public)
     * Gồm: link nhóm theo section + các cấu hình đang hoạt động
     */
    public FooterInfoResponse getFooterInfo() {
        // Lấy tất cả link đang hoạt động, nhóm theo section (giữ thứ tự enum)
        Map<FooterSection, List<FooterLinkResponse>> linksBySection = new LinkedHashMap<>();
        Arrays.stream(FooterSection.values()).forEach(section -> {
            List<FooterLinkResponse> links = footerLinkRepository
                    .findAllByIsActiveTrueAndSectionOrderBySortOrderAscLabelAsc(section)
                    .stream()
                    .map(this::mapToLinkResponse)
                    .collect(Collectors.toList());
            if (!links.isEmpty()) {
                linksBySection.put(section, links);
            }
        });

        // Lấy tất cả cấu hình đang hoạt động dưới dạng key -> value
        Map<String, String> settings = footerSettingRepository.findAllByIsActiveTrue()
                .stream()
                .collect(Collectors.toMap(
                        FooterSetting::getSettingKey,
                        setting -> setting.getSettingValue() != null ? setting.getSettingValue() : "",
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        return FooterInfoResponse.builder()
                .links(linksBySection)
                .settings(settings)
                .build();
    }

    /**
     * Lấy tất cả footer link (cho admin) với phân trang, tìm kiếm và lọc theo section
     */
    public Page<FooterLinkResponse> getAllFooterLinks(Pageable pageable, String search, FooterSection section) {
        Page<FooterLink> footerLinks;

        if (section != null) {
            footerLinks = footerLinkRepository.findBySection(section, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            footerLinks = footerLinkRepository.findBySearchQuery(search.trim(), pageable);
        } else {
            footerLinks = footerLinkRepository.findAllWithCreatedBy(pageable);
        }

        return footerLinks.map(this::mapToLinkResponse);
    }

    /**
     * Lấy footer link theo ID
     */
    public FooterLinkResponse getFooterLinkById(String id) {
        FooterLink footerLink = footerLinkRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FOOTER_LINK_NOT_FOUND));
        return mapToLinkResponse(footerLink);
    }

    /**
     * Tạo footer link mới
     */
    @Transactional
    public FooterLinkResponse createFooterLink(FooterLinkCreateRequest request) {
        if (footerLinkRepository.existsByLabelAndSection(request.getLabel(), request.getSection())) {
            throw new AppException(ErrorCode.FOOTER_LINK_EXISTED);
        }

        User admin = getCurrentAdmin();

        FooterLink footerLink = FooterLink.builder()
                .label(request.getLabel())
                .href(request.getHref())
                .section(request.getSection())
                .icon(request.getIcon())
                .isActive(request.getIsActive())
                .openNewTab(request.getOpenNewTab())
                .sortOrder(request.getSortOrder())
                .createdBy(admin)
                .createdByName(admin.getFullName() != null ? admin.getFullName() : admin.getEmail())
                .build();

        FooterLink savedFooterLink = footerLinkRepository.save(footerLink);
        log.info("✅ Đã tạo footer link mới: [{}] {} bởi admin: {}", savedFooterLink.getSection(), savedFooterLink.getLabel(), admin.getEmail());

        return mapToLinkResponse(savedFooterLink);
    }

    /**
     * Cập nhật footer link
     */
    @Transactional
    public FooterLinkResponse updateFooterLink(String id, FooterLinkUpdateRequest request) {
        FooterLink footerLink = footerLinkRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FOOTER_LINK_NOT_FOUND));

        if (footerLinkRepository.existsByLabelAndSectionAndIdNot(request.getLabel(), request.getSection(), id)) {
            throw new AppException(ErrorCode.FOOTER_LINK_EXISTED);
        }

        footerLink.setLabel(request.getLabel());
        footerLink.setHref(request.getHref());
        footerLink.setSection(request.getSection());
        footerLink.setIcon(request.getIcon());
        footerLink.setIsActive(request.getIsActive());
        footerLink.setOpenNewTab(request.getOpenNewTab());
        footerLink.setSortOrder(request.getSortOrder());

        FooterLink updatedFooterLink = footerLinkRepository.save(footerLink);
        log.info("✅ Đã cập nhật footer link: {}", updatedFooterLink.getLabel());

        return mapToLinkResponse(updatedFooterLink);
    }

    /**
     * Xóa footer link
     */
    @Transactional
    public void deleteFooterLink(String id) {
        FooterLink footerLink = footerLinkRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FOOTER_LINK_NOT_FOUND));

        footerLinkRepository.delete(footerLink);
        log.info("✅ Đã xóa footer link: {}", footerLink.getLabel());
    }

    /**
     * Lấy tất cả cấu hình footer (cho admin)
     */
    public List<FooterSettingResponse> getAllFooterSettings() {
        return footerSettingRepository.findAll()
                .stream()
                .map(this::mapToSettingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo cấu hình footer mới
     */
    @Transactional
    public FooterSettingResponse createFooterSetting(FooterSettingCreateRequest request) {
        if (footerSettingRepository.existsBySettingKey(request.getSettingKey())) {
            throw new AppException(ErrorCode.FOOTER_SETTING_KEY_EXISTED);
        }

        User admin = getCurrentAdmin();

        FooterSetting footerSetting = FooterSetting.builder()
                .settingKey(request.getSettingKey())
                .settingValue(request.getSettingValue())
                .isActive(request.getIsActive())
                .createdBy(admin)
                .createdByName(admin.getFullName() != null ? admin.getFullName() : admin.getEmail())
                .build();

        FooterSetting savedFooterSetting = footerSettingRepository.save(footerSetting);
        log.info("✅ Đã tạo cấu hình footer: {} bởi admin: {}", savedFooterSetting.getSettingKey(), admin.getEmail());

        return mapToSettingResponse(savedFooterSetting);
    }

    /**
     * Cập nhật cấu hình footer
     */
    @Transactional
    public FooterSettingResponse updateFooterSetting(String id, FooterSettingUpdateRequest request) {
        FooterSetting footerSetting = footerSettingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FOOTER_SETTING_NOT_FOUND));

        if (footerSettingRepository.existsBySettingKeyAndIdNot(request.getSettingKey(), id)) {
            throw new AppException(ErrorCode.FOOTER_SETTING_KEY_EXISTED);
        }

        footerSetting.setSettingKey(request.getSettingKey());
        footerSetting.setSettingValue(request.getSettingValue());
        footerSetting.setIsActive(request.getIsActive());

        FooterSetting updatedFooterSetting = footerSettingRepository.save(footerSetting);
        log.info("✅ Đã cập nhật cấu hình footer: {}", updatedFooterSetting.getSettingKey());

        return mapToSettingResponse(updatedFooterSetting);
    }

    /**
     * Xóa cấu hình footer
     */
    @Transactional
    public void deleteFooterSetting(String id) {
        FooterSetting footerSetting = footerSettingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FOOTER_SETTING_NOT_FOUND));

        footerSettingRepository.delete(footerSetting);
        log.info("✅ Đã xóa cấu hình footer: {}", footerSetting.getSettingKey());
    }

    /**
     * Lấy admin hiện tại từ JWT
     */
    private User getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = null;

        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            email = jwt.getSubject();
        }

        if (email == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /**
     * Chuyển đổi Entity sang Response DTO
     */
    private FooterLinkResponse mapToLinkResponse(FooterLink footerLink) {
        return FooterLinkResponse.builder()
                .id(footerLink.getId())
                .label(footerLink.getLabel())
                .href(footerLink.getHref())
                .section(footerLink.getSection())
                .icon(footerLink.getIcon())
                .isActive(footerLink.getIsActive())
                .openNewTab(footerLink.getOpenNewTab())
                .sortOrder(footerLink.getSortOrder())
                .createdAt(footerLink.getCreatedAt())
                .updatedAt(footerLink.getUpdatedAt())
                .createdByName(footerLink.getCreatedByName())
                .build();
    }

    /**
     * Chuyển đổi Entity sang Response DTO
     */
    private FooterSettingResponse mapToSettingResponse(FooterSetting footerSetting) {
        return FooterSettingResponse.builder()
                .id(footerSetting.getId())
                .settingKey(footerSetting.getSettingKey())
                .settingValue(footerSetting.getSettingValue())
                .isActive(footerSetting.getIsActive())
                .createdAt(footerSetting.getCreatedAt())
                .updatedAt(footerSetting.getUpdatedAt())
                .createdByName(footerSetting.getCreatedByName())
                .build();
    }
}