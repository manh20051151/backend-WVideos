package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.SiteSettingResponse;
import com.example.backendWVideos.entity.SiteSetting;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.SiteSettingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteSettingService {

    final SiteSettingRepository siteSettingRepository;

    public static final String KEY_SITE_LOGO = "SITE_LOGO";
    public static final String KEY_SITE_FAVICON = "SITE_FAVICON";

    private static final List<String> KNOWN_KEYS = List.of(KEY_SITE_LOGO, KEY_SITE_FAVICON);

    /**
     * Map key -> value cho FE hiển thị (public).
     */
    public Map<String, String> getPublicSettings() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : KNOWN_KEYS) {
            siteSettingRepository.findBySettingKey(key)
                    .filter(s -> s.getSettingValue() != null && !s.getSettingValue().isBlank())
                    .ifPresent(s -> result.put(key, s.getSettingValue()));
        }
        return result;
    }

    public String getLogoUrl() {
        return getValue(KEY_SITE_LOGO);
    }

    public String getFaviconUrl() {
        return getValue(KEY_SITE_FAVICON);
    }

    private String getValue(String key) {
        return siteSettingRepository.findBySettingKey(key)
                .map(SiteSetting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(null);
    }

    /**
     * Danh sách cấu hình cho admin.
     */
    public List<SiteSettingResponse> getAllForAdmin() {
        List<SiteSettingResponse> result = new ArrayList<>();
        for (String key : KNOWN_KEYS) {
            SiteSetting setting = siteSettingRepository.findBySettingKey(key).orElse(null);
            result.add(SiteSettingResponse.builder()
                    .settingKey(key)
                    .settingValue(setting != null ? setting.getSettingValue() : null)
                    .updatedAt(setting != null ? setting.getUpdatedAt() : null)
                    .build());
        }
        return result;
    }

    /**
     * Admin cập nhật giá trị cấu hình (value rỗng = xóa, dùng mặc định).
     */
    @Transactional
    public SiteSettingResponse update(String key, String value) {
        if (!KNOWN_KEYS.contains(key)) {
            throw new AppException(ErrorCode.SITE_SETTING_NOT_FOUND);
        }

        SiteSetting setting = siteSettingRepository.findBySettingKey(key).orElseGet(() ->
                SiteSetting.builder().settingKey(key).build());

        String trimmed = value != null ? value.trim() : "";
        setting.setSettingValue(trimmed.isEmpty() ? null : trimmed);
        setting.setUpdatedAt(LocalDateTime.now());
        siteSettingRepository.save(setting);

        log.info("Admin cập nhật site setting {}: {}", key, trimmed.isEmpty() ? "(mặc định)" : "URL mới");
        return SiteSettingResponse.builder()
                .settingKey(key)
                .settingValue(setting.getSettingValue())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
