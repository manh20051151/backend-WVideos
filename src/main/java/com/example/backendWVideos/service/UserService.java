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
import com.example.backendWVideos.mapper.UserMapper;
import com.example.backendWVideos.repository.RoleRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.PendingRegistrationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

        if(userRepository.existsByUsername(request.getUsername())){
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



    @PostAuthorize("returnObject.username == authentication.name")
    public UserResponse getUser(String id){
        // Load user kèm theo purchasedDocuments và roles
        User user = userRepository.findByIdWithRolesAndPurchasedDocuments(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toUserResponse(user);
    }

//    @PostAuthorize("returnObject.username == authentication.name")
    public UserResponse getUserNotoken(String id){
        // Load user kèm theo purchasedDocuments và roles
        User user = userRepository.findByIdWithRolesAndPurchasedDocuments(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toUserResponse(user);
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

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        log.info("Người dùng tự cập nhật thông tin - User: {}", user.getId());

        // Cập nhật qua mapper (chỉ thông tin cơ bản, không bao gồm password)
        userMapper.updateUserByUser(user, request);

        // Lưu thay đổi
        user = userRepository.save(user);
        log.info("Cập nhật thông tin cá nhân thành công - User: {}", user.getId());
        return userMapper.toUserResponse(user);
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

        // Tìm user theo email
        User user =  userRepository.findByEmail(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Load lại user kèm theo purchasedDocuments và roles
        user = userRepository.findByIdWithRolesAndPurchasedDocuments(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
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

//    @CircuitBreaker(name = "registration", fallbackMethod = "registrationFallback")
    @RateLimiter(name = "registration")
    @Retry(name = "registration")
    public void startRegistration(UserCreateRequest request) {
        // Kiểm tra username và email
        if (userRepository.existsByEmail(request.getEmail()) || 
            pendingRegistrationRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        // Tạo pending registration
        PendingRegistration registration = PendingRegistration.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .authProvider(AuthProvider.LOCAL)
                .numberPhone(request.getNumberPhone())
                .fullName(request.getFullName())
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusMinutes(expirationMinutes))
                .build();

        pendingRegistrationRepository.save(registration);
        sendConfirmationEmail(registration);
    }

    @CircuitBreaker(name = "emailSending", fallbackMethod = "emailSendingFallback")
    @Retry(name = "emailSending")
    private void sendConfirmationEmail(PendingRegistration registration) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("nguyenvietmanh1409@gmail.com");
            helper.setTo(registration.getEmail());
            helper.setSubject("Xác nhận đăng ký tài khoản");
            
            String confirmationUrl = frontendUrl + "/confirm-registration?token=" + registration.getToken();
            String emailContent = String.format("""
                <h2>Xác nhận đăng ký tài khoản</h2>
                <p>Xin chào %s,</p>
                <p>Vui lòng click vào link bên dưới để hoàn tất đăng ký tài khoản:</p>
                <a href="%s">Xác nhận đăng ký</a>
                <p>Link này sẽ hết hạn sau %d phút.</p>
                <p>Nếu bạn không yêu cầu đăng ký tài khoản, vui lòng bỏ qua email này.</p>
                """, registration.getUsername(), confirmationUrl, expirationMinutes);
            
            helper.setText(emailContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
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
                // Tìm user theo email (ưu tiên) hoặc username
                return userRepository.findByEmail(registration.getEmail())
                        .or(() -> userRepository.findByUsername(registration.getUsername()))
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

            // Kiểm tra nếu username đã tồn tại -> user đã được tạo với username này
            // Optional<User> existingUserByUsername = userRepository.findByUsername(registration.getUsername());
            // if (existingUserByUsername.isPresent()) {
            //     log.info("User với username {} đã tồn tại, cập nhật trạng thái registration", registration.getUsername());
            //     registration.setConfirmed(true);
            //     pendingRegistrationRepository.save(registration);
            //     return existingUserByUsername.get();
            // }

            // Tạo user mới
            User user = User.builder()
                    .username(registration.getUsername())
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
}
