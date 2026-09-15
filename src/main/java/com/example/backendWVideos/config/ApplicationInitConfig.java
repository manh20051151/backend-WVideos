package com.example.backendWVideos.config;

import com.example.backendWVideos.entity.NavItem;
import com.example.backendWVideos.entity.Role;
import com.example.backendWVideos.repository.NavItemRepository;
import com.example.backendWVideos.repository.RoleRepository;
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
                    { "Clip Sao Tạo Nội Dung", "/clip-sao-tao-noi-dung" },
                    { "Clip Sao Hát Nhép", "/clip-sao-hat-nhep" },
                    { "Ảnh Sao", "/anh-sao" },
                    { "Thể Loại", "/the-loai" },
                    { "Khác", "/khac" },
                    { "Đóng Góp", "/dong-gop" },
                    { "Thông báo", "/thong-bao" },
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
}
