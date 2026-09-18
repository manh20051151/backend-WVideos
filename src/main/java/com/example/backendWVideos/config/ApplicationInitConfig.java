package com.example.backendWVideos.config;

import com.example.backendWVideos.entity.FooterLink;
import com.example.backendWVideos.entity.FooterSetting;
import com.example.backendWVideos.entity.NavItem;
import com.example.backendWVideos.entity.Role;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.FooterSection;
import com.example.backendWVideos.repository.FooterLinkRepository;
import com.example.backendWVideos.repository.FooterSettingRepository;
import com.example.backendWVideos.repository.NavItemRepository;
import com.example.backendWVideos.repository.NotificationRepository;
import com.example.backendWVideos.repository.RoleRepository;
import com.example.backendWVideos.repository.SubscriptionRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoReactionRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationInitConfig {

    @Bean
    ApplicationRunner applicationRunner(RoleRepository roleRepository, NavItemRepository navItemRepository){
        return  args -> {
            roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name("ADMIN")
                            .description("Quản trị viên hệ thống")
                            .build()));

            roleRepository.findByName("GUEST")
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name("GUEST")
                            .description("Thành viên vãng lai")
                            .build()));

            // Đảm bảo 10 mục menu điều hướng mặc định luôn tồn tại (upsert theo slug)
            String[][] defaultNavItems = {
                    { "Tin tức", "/news" },
                    { "Shorts", "/shorts" },
                    { "Kênh Đã Đăng Ký", "/kenh-da-dang-ky" },
            };

            int order = (int) navItemRepository.count();
            for (String[] item : defaultNavItems) {
                String slug = item[1].substring(1); // bỏ dấu '/' đầu
                if (navItemRepository.findBySlug(slug).isEmpty()) {
                    navItemRepository.save(NavItem.builder()
                            .label(item[0])
                            .slug(slug)
                            .href(item[1])
                            .isActive(true)
                            .openNewTab(false)
                            .sortOrder(order++)
                            .createdByName("Hệ thống")
                            .build());
                }
            }
            log.info("Đã đảm bảo {} mục menu điều hướng mặc định tồn tại", defaultNavItems.length);
        };
    }

    // Đảm bảo dữ liệu footer mặc định luôn tồn tại (links + settings, upsert theo label/key)
    @Bean
    ApplicationRunner defaultFooterData(FooterLinkRepository footerLinkRepository,
                                        FooterSettingRepository footerSettingRepository) {
        return args -> {
            // Cấu hình footer mặc định
            String[][] defaultFooterSettings = {
                    { "brand_description", "Nền tảng chia sẻ video hàng đầu Việt Nam. Khám phá hàng triệu nội dung sáng tạo mỗi ngày." },
                    { "copyright_text", "WVideos. All rights reserved." },
            };

            int settingCount = 0;
            for (String[] item : defaultFooterSettings) {
                if (footerSettingRepository.findBySettingKey(item[0]).isEmpty()) {
                    footerSettingRepository.save(FooterSetting.builder()
                            .settingKey(item[0])
                            .settingValue(item[1])
                            .isActive(true)
                            .createdByName("Hệ thống")
                            .build());
                    settingCount++;
                }
            }

            // Link footer mặc định theo từng khu vực (label, href, section)
            Object[][] defaultFooterLinks = {
                    { "Trang chủ", "/", FooterSection.QUICK_LINKS },
                    { "Shorts", "/shorts", FooterSection.QUICK_LINKS },
                    { "Tin tức", "/news", FooterSection.QUICK_LINKS },
                    { "Thể loại", "/the-loai", FooterSection.QUICK_LINKS },
                    { "Kênh đã đăng ký", "/kenh-da-dang-ky", FooterSection.QUICK_LINKS },
                    { "Tin tức", "/news", FooterSection.CATEGORIES },
                    { "Shorts", "/shorts", FooterSection.CATEGORIES },
                    { "Clip Sao", "/clip-sao-tao-noi-dung", FooterSection.CATEGORIES },
                    { "Ảnh Sao", "/anh-sao", FooterSection.CATEGORIES },
                    { "Âm nhạc", "#", FooterSection.CATEGORIES },
                    { "Về chúng tôi", "#", FooterSection.SUPPORT },
                    { "Điều khoản sử dụng", "#", FooterSection.SUPPORT },
                    { "Chính sách bảo mật", "#", FooterSection.SUPPORT },
                    { "Trợ giúp", "#", FooterSection.SUPPORT },
                    { "Liên hệ", "#", FooterSection.SUPPORT },
                    { "Facebook", "#", FooterSection.SOCIAL },
                    { "YouTube", "#", FooterSection.SOCIAL },
                    { "TikTok", "#", FooterSection.SOCIAL },
                    { "Instagram", "#", FooterSection.SOCIAL },
                    { "Điều khoản", "#", FooterSection.BOTTOM },
                    { "Bảo mật", "#", FooterSection.BOTTOM },
                    { "Cookie", "#", FooterSection.BOTTOM },
            };

            int linkCount = 0;
            int order = 0;
            for (Object[] item : defaultFooterLinks) {
                String label = (String) item[0];
                String href = (String) item[1];
                FooterSection section = (FooterSection) item[2];
                if (!footerLinkRepository.existsByLabelAndSection(label, section)) {
                    footerLinkRepository.save(FooterLink.builder()
                            .label(label)
                            .href(href)
                            .section(section)
                            .isActive(true)
                            .openNewTab(false)
                            .sortOrder(order++)
                            .createdByName("Hệ thống")
                            .build());
                    linkCount++;
                }
            }

            if (settingCount > 0 || linkCount > 0) {
                log.info("Đã seed dữ liệu footer mặc định: {} cấu hình, {} link", settingCount, linkCount);
            }
        };
    }

    // Backfill joined_date còn thiếu cho các tài khoản cũ (để trang profile hiển thị "Ngày tham gia")
    @Bean
    ApplicationRunner joinedDateBackfill(UserRepository userRepository) {
        return args -> {
            try {
                int updated = userRepository.backfillMissingJoinedDates();
                if (updated > 0) {
                    log.info("Đã backfill joined_date cho {} tài khoản cũ", updated);
                }
            } catch (Exception e) {
                log.warn("Không thể backfill joined_date: {}", e.getMessage());
            }
        };
    }

    // Backfill slug còn thiếu cho các video cũ (để link /watch/{slug} hoạt động)
    @Bean
    ApplicationRunner videoSlugBackfill(VideoRepository videoRepository) {
        return args -> {
            try {
                int updated = 0;
                for (Video video : videoRepository.findAllBySlugIsNull()) {
                    String baseSlug = normalizeTitleToSlug(video.getTitle());
                    String slug = baseSlug;
                    int counter = 2;
                    while (videoRepository.existsBySlug(slug)) {
                        slug = baseSlug + "-" + counter;
                        counter++;
                    }
                    video.setSlug(slug);
                    videoRepository.save(video);
                    updated++;
                }
                if (updated > 0) {
                    log.info("Đã backfill slug cho {} video cũ", updated);
                }
            } catch (Exception e) {
                log.warn("Không thể backfill slug video: {}", e.getMessage());
            }
        };
    }

    // Chuyển tiêu đề video thành slug (bỏ dấu tiếng Việt, viết thường, dấu gạch ngang)
    private String normalizeTitleToSlug(String title) {
        if (title == null || title.isBlank()) {
            return "video-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        String normalized = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[đĐ]", "d")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        if (normalized.isEmpty()) {
            normalized = "video-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return normalized;
    }

    // Dọn dẹp bản ghi reaction trùng lặp (cùng user + video) do dữ liệu cũ
    @Bean
    ApplicationRunner reactionDataCleanup(VideoReactionRepository videoReactionRepository) {
        return args -> {
            try {
                videoReactionRepository.deleteDuplicateReactions();
            } catch (Exception e) {
                log.warn("Không thể dọn dẹp reaction trùng lặp: {}", e.getMessage());
            }
        };
    }

    // Dọn dẹp bản ghi đăng ký (subscription) trùng lặp (cùng subscriber + channel) do dữ liệu cũ
    @Bean
    ApplicationRunner subscriptionDataCleanup(SubscriptionRepository subscriptionRepository) {
        return args -> {
            try {
                subscriptionRepository.deleteDuplicateSubscriptions();
            } catch (Exception e) {
                log.warn("Không thể dọn dẹp subscription trùng lặp: {}", e.getMessage());
            }
        };
    }

    // Dọn dẹp thông báo (notification) trùng lặp (cùng recipient + type + relatedId) do dữ liệu cũ
    @Bean
    ApplicationRunner notificationDataCleanup(NotificationRepository notificationRepository) {
        return args -> {
            try {
                notificationRepository.deleteDuplicateNotifications();
            } catch (Exception e) {
                log.warn("Không thể dọn dẹp notification trùng lặp: {}", e.getMessage());
            }
        };
    }
}
