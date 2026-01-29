package com.example.backendWVideos.config;

import com.example.backendWVideos.dto.response.AuthenticationResponse;
import com.example.backendWVideos.entity.Role;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.RoleRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.service.AuthenticationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationService authenticationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuthenticationService authenticationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authenticationService = authenticationService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        try {
            // Lấy user bao gồm cả tài khoản bị khóa (bỏ qua @SQLRestriction)
            User user = userRepository.findByEmailIncludingLocked(email).orElse(null);

            // Nếu user tồn tại nhưng bị khóa -> không cho đăng nhập
            if (user != null && user.isLocked()) {
                throw new AppException(ErrorCode.USER_LOCKED);
            }
            
            if (user == null) {
                // Tạo user mới nếu chưa tồn tại hoàn toàn
                user = new User();
                user.setEmail(email);
                user.setFullName(name);
                user.setUsername(email);
                user.setAvatar(oAuth2User.getAttribute("picture"));

                // Thêm role GUEST cho user mới
                Role roleGuest = roleRepository.findByName("GUEST")
                        .orElseThrow(() -> new RuntimeException("Role GUEST not found"));
                Set<Role> roles = new HashSet<>();
                roles.add(roleGuest);
                user.setRoles(roles);

                userRepository.save(user);
            }

            // Tạo JWT token (hàm generateToken cũng sẽ kiểm tra locked lần nữa để đảm bảo an toàn)
            String token = authenticationService.generateToken(oAuth2User);
            
            // Tạo cookie cho token
            Cookie tokenCookie = new Cookie("auth_token", token);
            tokenCookie.setPath("/");
            tokenCookie.setHttpOnly(true);
            tokenCookie.setSecure(request.isSecure());
            tokenCookie.setMaxAge(3600); // 1 hour
            response.addCookie(tokenCookie);

            // Thêm CORS headers
            response.setHeader("Access-Control-Allow-Origin", frontendUrl);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type");

            // Redirect về frontend với token trong query parameter
            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                    .queryParam("token", token)
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception ex) {
            logger.error("OAuth2 authentication error", ex);

            String errorParam = "authentication_failed";
            if (ex instanceof AppException appEx && appEx.getErrorCode() == ErrorCode.USER_LOCKED) {
                // Trả lỗi cụ thể cho FE để hiển thị thông báo "Tài khoản bị khóa"
                errorParam = "user_locked";
            }

            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/oauth2/redirect?error=" + errorParam);
        }
    }
} 