package com.example.backendWVideos.controller;


import com.example.backendWVideos.dto.request.*;
import com.example.backendWVideos.dto.request.ChangePasswordRequest;
import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.FinancialHistoryResponse;
import com.example.backendWVideos.dto.response.UserFinancialInfoDTO;
import com.example.backendWVideos.dto.response.UserProfileResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.dto.response.UserResponse;
import com.example.backendWVideos.dto.response.VideoResponse;
import com.example.backendWVideos.mapper.UserMapper;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.service.UserService;
import com.example.backendWVideos.service.UserFinancialService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;
    UserMapper userMapper;
    UserRepository userRepository;
    UserFinancialService userFinancialService;
    
    /**
     * Xác thực quyền admin từ server - endpoint này chỉ trả về success nếu user có role ADMIN
     * Dùng để verify admin role từ frontend, tránh việc user sửa localStorage
     */
    @GetMapping("/verify-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Boolean>> verifyAdmin() {
        return ApiResponse.<Map<String, Boolean>>builder()
                .code(1000)
                .message("Xác thực quyền admin thành công")
                .result(Map.of("isAdmin", true))
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody UserCreateRequest request) {
        userService.startRegistration(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Vui lòng kiểm tra email để xác nhận đăng ký tài khoản")
                .build();
    }

    @GetMapping("/confirm")
    public ApiResponse<UserResponse> confirmRegistration(@RequestParam String token) {
        User user = userService.confirmRegistration(token);
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Xác nhận đăng ký thành công")
                .result(userMapper.toUserResponse(user))
                .build();
    }

    @GetMapping
    ApiResponse<List<UserResponse>> getUsers(){
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<org.springframework.data.domain.Page<UserResponse>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<org.springframework.data.domain.Page<UserResponse>>builder()
                .result(userService.searchUsers(keyword, page, size))
                .build();
    }


    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable String userId){
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }
    @GetMapping("/notoken/{userId}")
    ApiResponse<UserResponse> getUserNoToken(@PathVariable String userId){
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserNotoken(userId))
                .build();
    }

    @GetMapping("/my-info")
    ApiResponse<UserResponse> getMyInfo(){
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }



    @DeleteMapping("/{userId}")
    ApiResponse<String> deleteUser(@PathVariable String userId){
        userService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result("User hs been deleted")
                .build();
    }

    /**
     * Cập nhật thông tin người dùng (Admin)
     */
    @PutMapping("/{userId}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request) {
        
        UserResponse userResponse = userService.updateUserByAdmin(userId, request);
        
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Cập nhật thông tin người dùng thành công")
                .result(userResponse)
                .build();
    }
    
    /**
     * Người dùng tự cập nhật thông tin của mình
     */
    @PutMapping("/my-info")
    public ApiResponse<UserResponse> updateMyInfo(
            @Valid @RequestBody UserUpdateByUserRequest request) {
        
        UserResponse userResponse = userService.updateMyInfo(request);
        
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Cập nhật thông tin cá nhân thành công")
                .result(userResponse)
                .build();
    }
    
    /**
     * Đổi mật khẩu cho người dùng hiện tại
     */
    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        
        return userService.changePassword(request);
    }

    @PostMapping("/{userId}/lock")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> lockUser(
            @PathVariable String userId,
            @RequestParam String lockedById,
            @RequestParam String reason) {

        userService.lockUser(userId, lockedById, reason);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã khóa tài khoản thành công")
                .build();
    }

    @PostMapping("/{userId}/unlock")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> unlockUser(@PathVariable String userId) {
        userService.unlockUser(userId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã mở khóa tài khoản thành công")
                .build();
    }

    @PostMapping("/{userId}/comment-ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.example.backendWVideos.dto.response.UserResponse> banUserCommenting(
            @PathVariable String userId,
            @RequestBody @Valid com.example.backendWVideos.dto.request.CommentBanRequest request) {

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        com.example.backendWVideos.dto.response.UserResponse response =
                userService.banUserCommenting(adminEmail, userId, request);

        return ApiResponse.<com.example.backendWVideos.dto.response.UserResponse>builder()
                .code(1000)
                .result(response)
                .message("Đã khóa bình luận của người dùng trong " + request.getHours() + " giờ")
                .build();
    }

    @DeleteMapping("/{userId}/comment-ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.example.backendWVideos.dto.response.UserResponse> removeCommentBan(
            @PathVariable String userId) {

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        com.example.backendWVideos.dto.response.UserResponse response =
                userService.removeCommentBan(adminEmail, userId);

        return ApiResponse.<com.example.backendWVideos.dto.response.UserResponse>builder()
                .code(1000)
                .result(response)
                .message("Đã mở khóa bình luận cho người dùng")
                .build();
    }

    @GetMapping("/locked")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<UserResponse>> getLockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<UserResponse>>builder()
                .code(1000)
                .message("Lấy danh sách tài khoản bị khóa thành công")
                .result(userService.getLockedUsers(pageable))
                .build();
    }



    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            throw new AppException(ErrorCode.INVALID_DATA);
        }

        userService.forgotPassword(request.getEmail());
        // Thông báo chung cho mọi trường hợp để không lộ email nào có tài khoản
        return ApiResponse.<Void>builder()
                .message("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            throw new AppException(ErrorCode.INVALID_DATA);
        }

        userService.resetPassword(request.getToken(), request.getPassword());
        return ApiResponse.<Void>builder()
                .message("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới.")
                .build();
    }
    
    /**
     * Cập nhật thông tin ngân hàng của người dùng hiện tại
     */
    @PutMapping("/my-bank-info")
    public ApiResponse<UserResponse> updateMyBankInfo(
            @Valid @RequestBody BankInfoUpdateRequest request) {
        
        UserResponse userResponse = userService.updateMyBankInfo(request);
        
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Cập nhật thông tin ngân hàng thành công")
                .result(userResponse)
                .build();
    }
    
    /**
     * Cập nhật thông tin ngân hàng của user theo ID (Admin)
     */
    @PutMapping("/{userId}/bank-info")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUserBankInfo(
            @PathVariable String userId,
            @Valid @RequestBody BankInfoUpdateRequest request) {
        
        UserResponse userResponse = userService.updateBankInfo(userId, request);
        
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Cập nhật thông tin ngân hàng thành công")
                .result(userResponse)
                .build();
    }
    
    /**
     * Xóa thông tin ngân hàng của user (Admin)
     */
    @DeleteMapping("/{userId}/bank-info")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> clearUserBankInfo(@PathVariable String userId) {
        
        UserResponse userResponse = userService.clearBankInfo(userId);
        
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Xóa thông tin ngân hàng thành công")
                .result(userResponse)
                .build();
    }

    @GetMapping("/my-financial-history")
    @Operation(summary = "Lịch sử tài chính cá nhân", description = "Biến động số dư, doanh thu, thống kê theo tháng của chính user đăng nhập")
    public ApiResponse<FinancialHistoryResponse> getMyFinancialHistory() {
        return ApiResponse.<FinancialHistoryResponse>builder()
                .code(1000)
                .message("Lấy lịch sử tài chính thành công")
                .result(userFinancialService.getMyFinancialHistory())
                .build();
    }

    @GetMapping("/{userId}/financial-info")
    public ApiResponse<UserFinancialInfoDTO> getUserFinancialInfo(@PathVariable String userId) {
        // Chỉ cho phép user xem thông tin tài chính của chính mình (chống truy cập dữ liệu người khác)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User current = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (!current.getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        UserFinancialInfoDTO financialInfo = userFinancialService.getUserFinancialInfo(userId);
        return ApiResponse.<UserFinancialInfoDTO>builder()
                .code(1000)
                .message("Lấy thông tin tài chính thành công")
                .result(financialInfo)
                .build();
    }
    
    /**
     * Lấy thông tin profile của user (dùng cho trang channel)
     */
    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .message("OK")
                .result(profile)
                .build();
    }

    /**
     * Lấy danh sách video public của kênh, phân trang (trang channel "Xem thêm")
     */
    @GetMapping("/{userId}/videos")
    public ApiResponse<Page<VideoResponse>> getChannelVideos(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Page<VideoResponse> videos = userService.getChannelVideos(userId, page, size);
        return ApiResponse.<Page<VideoResponse>>builder()
                .code(1000)
                .message("OK")
                .result(videos)
                .build();
    }

}
