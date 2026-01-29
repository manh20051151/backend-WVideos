package com.example.backendWVideos.config;

import com.example.backendWVideos.entity.Role;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.repository.RoleRepository;
import com.example.backendWVideos.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository){
        return  args -> {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> {
                        Role newAdminRole = Role.builder()
                                .name("ADMIN")
                                .description("Quản trị viên hệ thống")
                                .build();

                        // Có thể thêm permissions mặc định ở đây nếu cần
                        // newAdminRole.setPermissions(defaultAdminPermissions());

                        return roleRepository.save(newAdminRole);
                    });
            Role adminGuest = roleRepository.findByName("GUEST")
                    .orElseGet(() -> {
                        Role newAdminRole = Role.builder()
                                .name("GUEST")
                                .description("Thành viên vãng lai")
                                .build();

                        // Có thể thêm permissions mặc định ở đây nếu cần
                        // newAdminRole.setPermissions(defaultAdminPermissions());

                        return roleRepository.save(newAdminRole);
                    });
//            if (userRepository.findByUsername("admin").isEmpty()){
////                var roles = new HashSet<String>();
////                roles.add(Role.ADMIN.name());
//                Role roleUser = roleRepository.findByName("ADMIN")
//                        .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));
//
//                Set<Role> roles = new HashSet<>();
//                roles.add(roleUser);
//                User user = User.builder()
//                        .username("admin")
//                        .password(passwordEncoder.encode("admin"))
//                        .roles(roles)
//                        .build();
//                userRepository.save(user);
//                log.warn("Đã tạo một user admin mặc định (password: admin)");
//            }
        };
    }
}
