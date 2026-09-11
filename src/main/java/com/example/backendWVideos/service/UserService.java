package com.example.backendWVideos.service;


import com.example.backendWVideos.entity.Role;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.PendingRegistration;
import com.example.backendWVideos.enums.AuthProvider;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.dto.request.BankInfoUpdateRequest;
import com.example.backendWVideos.dto.request.ChangePasswordRequest;
import com.example.backendWVideos.dto.request.UserCreateRequest;
import com.example.backendWVideos.dto.request.UserUpdateByUserRequest;
import com.example.backendWVideos.dto.request.UserUpdateRequest;
import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.UserResponse;
import com.example.backendWVideos.dto.response.UserProfileResponse;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.entity.Role;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.entity.PendingRegistration;
import com.example.backendWVideos.enums.AuthProvider;
import com.example.backendWVideos.enums.VideoStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.dto.request.BankInfoUpdateRequest;
import com.example.backendWVideos.dto.request.ChangePasswordRequest;
import com.example.backendWVideos.dto.request.UserCreateRequest;
import com.example.backendWVideos.dto.request.UserUpdateByUserRequest;
import com.example.backendWVideos.dto.request.UserUpdateRequest;
import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.mapper.UserMapper;
import com.example.backendWVideos.mapper.VideoMapper;
import com.example.backendWVideos.repository.RoleRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.PendingRegistrationRepository;
import com.example.backendWVideos.repository.VideoRepository;
import com.example.backendWVideos.repository.SubscriptionRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserService {

    final UserRepository userRepository;
    final RoleRepository roleRepository;
    final UserMapper userMapper;
    final PasswordEncoder passwordEncoder;
    final JavaMailSender mailSender;
    final PendingRegistrationRepository pendingRegistrationRepository;
    final VideoRepository videoRepository;
    final VideoMapper videoMapper;
    final SubscriptionRepository subscriptionRepository;

    @Value("${app.registration.token.expiration-minutes:30}")
    int expirationMinutes;
    
    @Value("${app.frontend-url:http://localhost:3000}")
    String frontendUrl;

    private static final String EMAIL_SUBJECT = "Khôi phục mật khẩu";
    private static final String EMAIL_CONTENT = """
        <p>Xin chào,</p>
        <p>Bạn đã yêu cầu khôi phục mật khẩu. Dưới đây là mật khẩu mới của bạn:</p>
        <p><strong>%s</strong></p>
        <p>Vui lòng đăng nhập và thay đổi mật khẩu ngay sau khi nhận được email này.</p>
        <p>Trân trọng,</p>
        <p>Hệ thống</p>
        """;

    private static final String DEFAULT_AVATAR_URL =
            "https://res.cloudinary.com/dnvtmbmne/image/upload/v1744707484/et5vc9r9fejjgrjsvxyn.jpg";
    public User createUser(UserCreateRequest request) throws IOException {

        if(userRepository.existsByEmail(request.getEmail())){
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        User user = userMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Set avatar mặc định
        user.setAvatar(DEFAULT_AVATAR_URL);

//        HashSet<String> roles = new HashSet<>();
//        roles.add(Role.USER.name());
//        user.setRoles(roles);

        Role roleUser = roleRepository.findByName("GUEST")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(roleUser);

        user.setRoles(roles);

        User userResponse =   userRepository.save(user);

        userResponse =   userRepository.save(user);
        return userResponse;
    }


//    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers(){
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse).toList();
    }



    @PostAuthorize("returnObject.email == authentication.name")
    public UserResponse getUser(String id){
        // Load user kèm theo purchasedDocuments và roles
        User user = userRepository.findByIdWithRolesAndPurchasedDocuments(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return withSubscriberCount(userMapper.toUserResponse(user), user.getId());
    }

    public UserResponse getUserNotoken(String id){
        // Load user kèm theo purchasedDocuments và roles
        User user = userRepository.findByIdWithRolesAndPurchasedDocuments(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return withSubscriberCount(userMapper.toUserResponse(user), user.getId());
    }

    // Gắn số người đăng ký kênh vào UserResponse
    private UserResponse withSubscriberCount(UserResponse response, String userId) {
        response.setSubscriberCount(subscriptionRepository.countByChannelId(userId));
        return response;
    }



    public void deleteUser(String userId){
        userRepository.deleteById(userId);
    }

    /**
     * Cập nhật thông tin người dùng (Admin)
     */
    @Transactional
    public UserResponse updateUserByAdmin(String userId, UserUpdateRequest request) {
        log.info("[ADMIN] Cập nhật thông tin user - ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Cập nhật các trường cơ bản qua mapper
        userMapper.updateUser(user, request);

        // Xử lý cập nhật mật khẩu nếu có
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            log.info("[ADMIN] Đã cập nhật mật khẩu cho user {}", userId);
        }

        // Cập nhật roles nếu có (theo tên role trong request)
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> newRoles = request.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND)))
                    .collect(Collectors.toSet());
            user.setRoles(newRoles);
            log.info("[ADMIN] Đã cập nhật roles cho user {}: {}", userId, request.getRoles());
        }

        // Lưu thay đổi
        user = userRepository.save(user);
        log.info("[ADMIN] Cập nhật thông tin user thành công - ID: {}", userId);
        return userMapper.toUserResponse(user);
    }

    /**
     * Người dùng tự cập nhật thông tin của mình
     */
    @Transactional
    public UserResponse updateMyInfo(UserUpdateByUserRequest request) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        log.info("Người dùng tự cập nhật thông tin - Email: {}", email);

        // Cập nhật bằng native query để bỏ qua @SQLRestriction
        userRepository.updateUserInfo(
                email,
                request.getFullName(),
                request.getNumberPhone(),
                request.getGender(),
                request.getAvatar(),
                request.getBankName(),
                request.getBankAccountHolderName(),
                request.getBankAccountNumber()
        );
        
        log.info("Cập nhật thông tin cá nhân thành công - Email: {}", email);
        
        // Load lại user mới nhất bằng native query
        return getMyInfo();
    }

    /**
     * Đổi mật khẩu cho người dùng hiện tại
     */
    @Transactional
    public ApiResponse<Void> changePassword(ChangePasswordRequest request) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        log.info("Người dùng yêu cầu đổi mật khẩu - User: {}", user.getId());

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getPasswordOld(), user.getPassword())) {
            log.error("Mật khẩu cũ không đúng - User: {}", user.getId());
            throw new AppException(ErrorCode.INVALID_DATA);
        }

        // Kiểm tra mật khẩu mới không được trùng với mật khẩu cũ
        if (passwordEncoder.matches(request.getPasswordNew(), user.getPassword())) {
            log.error("Mật khẩu mới không được trùng với mật khẩu cũ - User: {}", user.getId());
            throw new AppException(ErrorCode.INVALID_DATA);
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getPasswordNew()));
        userRepository.save(user);
        
        log.info("Đổi mật khẩu thành công - User: {}", user.getId());
        
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đổi mật khẩu thành công")
                .build();
    }

    public UserResponse getMyInfo(){
        var context =  SecurityContextHolder.getContext();
        String name =  context.getAuthentication().getName();

        // Dùng native query để bỏ qua @SQLRestriction và lấy user kèm roles
        List<Object[]> userRows = userRepository.findUserWithRolesByEmail(name);
        
        if (userRows.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        
        Object[] firstRow = userRows.get(0);
        
        // Build user từ kết quả native query
        User user = User.builder()
                .id(String.valueOf(firstRow[0]))
                .password(firstRow[1] != null ? String.valueOf(firstRow[1]) : null)
                .numberPhone(firstRow[2] != null ? String.valueOf(firstRow[2]) : null)
                .fullName(firstRow[3] != null ? String.valueOf(firstRow[3]) : null)
                .avatar(firstRow[4] != null ? String.valueOf(firstRow[4]) : null)
                .email(firstRow[5] != null ? String.valueOf(firstRow[5]) : null)
                .gender(firstRow[6] != null)
                .bankName(firstRow[7] != null ? String.valueOf(firstRow[7]) : null)
                .bankAccountHolderName(firstRow[8] != null ? String.valueOf(firstRow[8]) : null)
                .bankAccountNumber(firstRow[9] != null ? String.valueOf(firstRow[9]) : null)
                .balance(firstRow[10] != null ? ((Number) firstRow[10]).doubleValue() : 0.0)
                .revenue(firstRow[11] != null ? ((Number) firstRow[11]).doubleValue() : 0.0)
                .build();

        // Load roles từ các row còn lại
        Set<Role> roles = new HashSet<>();
        for (Object[] row : userRows) {
            if (row[12] != null) { // role_id
                Role role = Role.builder()
                        .id(String.valueOf(row[12]))
                        .name(row[13] != null ? String.valueOf(row[13]) : null)
                        .description(row[14] != null ? String.valueOf(row[14]) : null)
                        .build();
                roles.add(role);
            }
        }
        user.setRoles(roles);

        UserResponse response = userMapper.toUserResponse(user);
        response.setSubscriberCount(subscriptionRepository.countByChannelId(user.getId()));
        return response;
    }




    // Khóa tài khoản
    @Transactional
    public void lockUser(String userId, String lockedById, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User lockedBy = userRepository.findById(lockedById)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.lock(lockedBy, reason);
        userRepository.save(user);
    }

    // Mở khóa tài khoản
    @Transactional
    public void unlockUser(String userId) {
        User user = userRepository.findLockedUserById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_LOCKED));

        user.unlock();
        userRepository.save(user);
    }

    // Lấy danh sách tài khoản bị khóa
    @Transactional(readOnly = true)
    public Page<UserResponse> getLockedUsers(Pageable pageable) {
        return userRepository.findLockedUsers(pageable)
                .map(userMapper::toUserResponse);
    }

    public void resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        sendNewPasswordEmail(user.getEmail(), newPassword);
    }

    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(8);
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }

    private void sendNewPasswordEmail(String toEmail, String newPassword) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("nguyenvietmanh1409@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject(EMAIL_SUBJECT);
            helper.setText(String.format(EMAIL_CONTENT, newPassword), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
            throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
        }
    }

    public void startRegistration(UserCreateRequest request) {
        // Kiểm tra user đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        // Nếu đã có pending registration chưa xác nhận -> cập nhật (nếu hết hạn) và gửi lại email
        // thay vì báo lỗi (tránh lỗi dương tính giả khi retry)
        PendingRegistration registration;
        Optional<PendingRegistration> existing = pendingRegistrationRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            registration = existing.get();
            if (registration.isExpired()) {
                registration.setToken(UUID.randomUUID().toString());
                registration.setExpiryDate(LocalDateTime.now().plusMinutes(expirationMinutes));
                registration.setPassword(passwordEncoder.encode(request.getPassword()));
                registration.setNumberPhone(request.getNumberPhone());
                registration.setFullName(request.getFullName());
                registration.setConfirmed(false);
            }
        } else {
            registration = PendingRegistration.builder()
                    .password(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail())
                    .authProvider(AuthProvider.LOCAL)
                    .numberPhone(request.getNumberPhone())
                    .fullName(request.getFullName())
                    .token(UUID.randomUUID().toString())
                    .expiryDate(LocalDateTime.now().plusMinutes(expirationMinutes))
                    .build();
        }

        pendingRegistrationRepository.save(registration);
        sendConfirmationEmail(registration);
    }

    @CircuitBreaker(name = "emailSending", fallbackMethod = "emailSendingFallback")
    @Retry(name = "emailSending")
    private void sendConfirmationEmail(PendingRegistration registration) {
        String confirmationUrl = frontendUrl + "/confirm-registration?token=" + registration.getToken();
        String emailContent = String.format("""
            <h2>Xác nhận đăng ký tài khoản</h2>
            <p>Xin chào %s,</p>
            <p>Vui lòng click vào link bên dưới để hoàn tất đăng ký tài khoản:</p>
            <a href="%s">Xác nhận đăng ký</a>
            <p>Link này sẽ hết hạn sau %d phút.</p>
            <p>Nếu bạn không yêu cầu đăng ký tài khoản, vui lòng bỏ qua email này.</p>
            """, registration.getEmail(), confirmationUrl, expirationMinutes);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("nguyenvietmanh1409@gmail.com");
            helper.setTo(registration.getEmail());
            helper.setSubject("Xác nhận đăng ký tài khoản");
            helper.setText(emailContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            // SMTP thất bại (vd: sai Gmail App Password) -> không chặn đăng ký,
            // in toàn bộ email ra console để dev xác nhận thủ công bằng link.
            log.warn("Email xác nhận KHÔNG gửi được ({}) - dùng nội dung dưới đây thay cho email thật:", e.getMessage());
            log.warn("To: {}\nSubject: Xác nhận đăng ký tài khoản\n\n{}", registration.getEmail(), emailContent);
        }
    }

    @CircuitBreaker(name = "confirmation", fallbackMethod = "confirmationFallback")
    @RateLimiter(name = "confirmation")
    @Transactional
    public User confirmRegistration(String token) {
        try {
            PendingRegistration registration = pendingRegistrationRepository.findByToken(token)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

            if (registration.isExpired()) {
                pendingRegistrationRepository.delete(registration);
                throw new AppException(ErrorCode.TOKEN_EXPIRED);
            }

            // Nếu đã confirm trước đó, trả về user hiện có
            if (registration.isConfirmed()) {
                // Tìm user theo email
                return userRepository.findByEmail(registration.getEmail())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            }

            // Kiểm tra nếu email đã tồn tại -> user đã được tạo, chỉ cần update registration status
            Optional<User> existingUserByEmail = userRepository.findByEmail(registration.getEmail());
            if (existingUserByEmail.isPresent()) {
                log.info("User với email {} đã tồn tại, cập nhật trạng thái registration", registration.getEmail());
                registration.setConfirmed(true);
                pendingRegistrationRepository.save(registration);
                return existingUserByEmail.get();
            }

            // Tạo user mới
            User user = User.builder()
                    .password(registration.getPassword())
                    .email(registration.getEmail())
                    .authProvider(registration.getAuthProvider())
                    .numberPhone(registration.getNumberPhone())
                    .fullName(registration.getFullName())
                    .avatar(DEFAULT_AVATAR_URL)
                    .build();

            // Thêm role GUEST
            Role roleGuest = roleRepository.findByName("GUEST")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            user.setRoles(Set.of(roleGuest));

            // Lưu user và cập nhật trạng thái registration
            User savedUser = userRepository.save(user);
            registration.setConfirmed(true);
            pendingRegistrationRepository.save(registration);

            log.info("Đăng ký thành công cho user: {}", savedUser.getEmail());
            return savedUser;

        } catch (Exception e) {
            log.error("Lỗi khi xác nhận đăng ký: ", e);
            if (e instanceof AppException) {
                throw e;
            }
            throw new AppException(ErrorCode.CONFIRMATION_FAILED);
        }
    }


    // Fallback methods
    private void registrationFallback(UserCreateRequest request, Exception e) {
        throw new AppException(ErrorCode.REGISTRATION_FAILED);
    }

    private void emailSendingFallback(PendingRegistration registration, Exception e) {
        throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
    }

    private User confirmationFallback(String token, Exception e) {
        throw new AppException(ErrorCode.CONFIRMATION_FAILED);
    }
    
    /**
     * Cập nhật thông tin ngân hàng của người dùng
     * 
     * @param userId ID người dùng cần cập nhật
     * @param bankInfoRequest Thông tin ngân hàng mới
     * @return Thông tin người dùng đã được cập nhật
     */
    @Transactional
    public UserResponse updateBankInfo(String userId, BankInfoUpdateRequest bankInfoRequest) {
        log.info("Cập nhật thông tin ngân hàng cho user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Cập nhật thông tin ngân hàng
        user.setBankName(bankInfoRequest.getBankName());
        user.setBankAccountHolderName(bankInfoRequest.getBankAccountHolderName());
        user.setBankAccountNumber(bankInfoRequest.getBankAccountNumber());
        
        user = userRepository.save(user);
        
        log.info("Cập nhật thông tin ngân hàng thành công - User: {}, Ngân hàng: {}, STK: {}", 
                userId, bankInfoRequest.getBankName(), maskBankAccountNumber(bankInfoRequest.getBankAccountNumber()));
        
        return userMapper.toUserResponse(user);
    }
    
    /**
     * Cập nhật thông tin ngân hàng của người dùng hiện tại
     */
    @Transactional
    public UserResponse updateMyBankInfo(BankInfoUpdateRequest bankInfoRequest) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        return updateBankInfo(user.getId(), bankInfoRequest);
    }
    
    /**
     * Xóa thông tin ngân hàng của người dùng
     */
    @Transactional
    public UserResponse clearBankInfo(String userId) {
        log.info("Xóa thông tin ngân hàng cho user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        user.setBankName(null);
        user.setBankAccountHolderName(null);
        user.setBankAccountNumber(null);
        
        user = userRepository.save(user);
        
        log.info("Xóa thông tin ngân hàng thành công - User: {}", userId);
        
        return userMapper.toUserResponse(user);
    }
    
    /**
     * Che giấu số tài khoản ngân hàng trong log (chỉ hiện 4 số cuối)
     */
    private String maskBankAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
    
    /**
     * Lấy thông tin profile của user (dùng cho trang channel)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Đếm số video
        long videoCount = videoRepository.countByUserIdAndStatusNot(userId, VideoStatus.DELETED);
        
        // Tính tổng lượt xem
        Long totalViews = videoRepository.getTotalViewsByUserId(userId, VideoStatus.DELETED);
        if (totalViews == null) totalViews = 0L;
        
        // Đếm số người đăng ký
        long subscriberCount = subscriptionRepository.countByChannelId(userId);
        
        // Lấy danh sách video (public only, không bị xóa)
        Page<Video> videos = videoRepository.findByUserIdAndStatusAndIsPublicTrue(userId, VideoStatus.READY, PageRequest.of(0, 20));
        List<VideoResponse> videoResponses = videos.getContent().stream()
                .map(videoMapper::toVideoResponse)
                .toList();
        
        // Kiểm tra user hiện tại đã đăng ký chưa
        Boolean isSubscribed = null;
        try {
            var context = SecurityContextHolder.getContext();
            String email = context.getAuthentication().getName();
            User currentUser = userRepository.findByEmail(email).orElse(null);
            if (currentUser != null && !currentUser.getId().equals(userId)) {
                isSubscribed = subscriptionRepository.existsBySubscriberIdAndChannelId(currentUser.getId(), userId);
            }
        } catch (Exception e) {
            // User chưa đăng nhập
        }
        
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .subscriberCount(subscriberCount)
                .videoCount(videoCount)
                .totalViews(totalViews)
                .isSubscribed(isSubscribed)
                .videos(videoResponses)
                .build();
    }
}
