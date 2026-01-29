package com.example.backendWVideos.controller;


import com.example.backendWVideos.dto.request.*;
import com.example.backendWVideos.dto.request.ChangePasswordRequest;
import com.example.backendWVideos.dto.response.DocumentResponseDTO;
import com.example.backendWVideos.dto.response.PurchasedDocumentResponse;
import com.example.backendWVideos.dto.response.SellerTransactionResponse;
import com.example.backendWVideos.enums.DocumentStatus;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.dto.response.UserResponse;
import com.example.backendWVideos.mapper.UserMapper;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;
    UserMapper userMapper;
    private final UserRepository userRepository;
    
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

    @GetMapping("/myInfo")
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
    
    /**
     * Lấy danh sách tài liệu đã mua của người dùng hiện tại
     */
    @GetMapping("/my-purchased-documents")
    public ApiResponse<Page<PurchasedDocumentResponse>> getMyPurchasedDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PurchasedDocumentResponse> purchasedDocuments = userService.getMyPurchasedDocuments(pageable);
        
        return ApiResponse.<Page<PurchasedDocumentResponse>>builder()
                .code(1000)
                .message("Lấy danh sách tài liệu đã mua thành công")
                .result(purchasedDocuments)
                .build();
    }
    
    /**
     * Lấy danh sách tài liệu đã đăng của người dùng hiện tại
     */
    @GetMapping("/my-documents")
    public ApiResponse<Page<DocumentResponseDTO>> getMyDocuments(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentResponseDTO> myDocuments = userService.getMyDocuments(status, pageable);
        
        String statusMessage = status != null ? 
                "theo trạng thái " + status.getVietnameseName() : "tất cả";
        
        return ApiResponse.<Page<DocumentResponseDTO>>builder()
                .code(1000)
                .message("Lấy danh sách tài liệu đã đăng " + statusMessage + " thành công")
                .result(myDocuments)
                .build();
    }
    
    /**
     * Lấy danh sách giao dịch bán hàng của người dùng hiện tại
     */
    @GetMapping("/my-sales-transactions")
    public ApiResponse<Page<SellerTransactionResponse>> getMySalesTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SellerTransactionResponse> salesTransactions = userService.getMySalesTransactions(pageable);
        
        return ApiResponse.<Page<SellerTransactionResponse>>builder()
                .code(1000)
                .message("Lấy danh sách giao dịch bán hàng thành công")
                .result(salesTransactions)
                .build();
    }
    
    /**
     * Lấy danh sách giao dịch mua hàng của người dùng hiện tại
     */
    @GetMapping("/my-purchase-transactions")
    public ApiResponse<Page<PurchasedDocumentResponse>> getMyPurchaseTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PurchasedDocumentResponse> purchaseTransactions = userService.getMyPurchaseTransactions(pageable);
        
        return ApiResponse.<Page<PurchasedDocumentResponse>>builder()
                .code(1000)
                .message("Lấy danh sách giao dịch mua hàng thành công")
                .result(purchaseTransactions)
                .build();
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

        userService.resetPassword(request.getEmail());
        return ApiResponse.<Void>builder()
                .message("Mật khẩu mới đã được gửi đến email của bạn")
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

}
