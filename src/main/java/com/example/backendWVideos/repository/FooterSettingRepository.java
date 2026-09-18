package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.FooterSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FooterSettingRepository extends JpaRepository<FooterSetting, String> {

    // Tìm theo key
    Optional<FooterSetting> findBySettingKey(String settingKey);

    // Lấy tất cả cấu hình đang hoạt động
    List<FooterSetting> findAllByIsActiveTrue();

    // Kiểm tra key đã tồn tại (trừ ID hiện tại)
    boolean existsBySettingKeyAndIdNot(String settingKey, String id);

    // Kiểm tra key đã tồn tại
    boolean existsBySettingKey(String settingKey);
}