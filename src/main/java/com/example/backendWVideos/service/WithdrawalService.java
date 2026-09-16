package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.request.WithdrawalCreateRequest;
import com.example.backendWVideos.dto.request.WithdrawalStatusUpdateRequest;
import com.example.backendWVideos.dto.response.WithdrawalResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.WithdrawalRequest;
import com.example.backendWVideos.enums.WithdrawalStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;

    // Doanh thu tối thiểu và số tiền rút tối thiểu (VND)
    private static final double MIN_WITHDRAWAL_AMOUNT = 500000.0;

    /**
     * Tạo yêu cầu rút tiền cho người dùng hiện tại.
     * Điều kiện: doanh thu hiện tại >= 500.000, đã cập nhật thông tin ngân hàng,
     * và không còn yêu cầu rút tiền nào đang chờ xử lý.
     */
    @Transactional
    public WithdrawalResponse createMyWithdrawal(WithdrawalCreateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Không cho tạo yêu cầu mới nếu yêu cầu trước chưa được xử lý
        if (withdrawalRequestRepository.existsByUserIdAndStatus(user.getId(), WithdrawalStatus.PENDING)) {
            throw new AppException(ErrorCode.WITHDRAWAL_REQUEST_PENDING_EXISTS);
        }

        // Bắt buộc đã cập nhật thông tin ngân hàng
        if (isBlank(user.getBankName())
                || isBlank(user.getBankAccountHolderName())
                || isBlank(user.getBankAccountNumber())) {
            throw new AppException(ErrorCode.BANK_INFO_NOT_FOUND);
        }

        double amount = request.getAmount() != null ? request.getAmount() : 0.0;
        double revenue = user.getRevenue() != null ? user.getRevenue() : 0.0;

        if (amount < MIN_WITHDRAWAL_AMOUNT) {
            throw new AppException(ErrorCode.WITHDRAWAL_AMOUNT_TOO_LOW);
        }
        // Doanh thu hiện tại phải đạt mức tối thiểu
        if (revenue < MIN_WITHDRAWAL_AMOUNT || amount > revenue) {
            throw new AppException(ErrorCode.INSUFFICIENT_REVENUE);
        }

        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .userId(user.getId())
                .amount(amount)
                .bankName(user.getBankName())
                .bankAccountHolderName(user.getBankAccountHolderName())
                .bankAccountNumber(user.getBankAccountNumber())
                .status(WithdrawalStatus.PENDING)
                .build();

        withdrawal = withdrawalRequestRepository.save(withdrawal);
        log.info("Tạo yêu cầu rút tiền thành công - User: {}, Số tiền: {}", user.getId(), amount);
        return toResponse(withdrawal, user);
    }

    /**
     * Danh sách yêu cầu rút tiền của người dùng hiện tại.
     */
    @Transactional(readOnly = true)
    public List<WithdrawalResponse> getMyWithdrawals() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return withdrawalRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(w -> toResponse(w, user))
                .toList();
    }

    /**
     * Danh sách toàn bộ yêu cầu rút tiền (Admin), lọc theo trạng thái nếu có.
     */
    @Transactional(readOnly = true)
    public Page<WithdrawalResponse> getAllWithdrawals(int page, int size, String status) {
        int limit = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), limit);

        Page<WithdrawalRequest> requests;
        if (status != null && !status.isBlank()) {
            WithdrawalStatus statusEnum;
            try {
                statusEnum = WithdrawalStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_STATUS);
            }
            requests = withdrawalRequestRepository.findByStatusOrderByCreatedAtDesc(statusEnum, pageable);
        } else {
            requests = withdrawalRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Map<String, User> userMap = new HashMap<>();
        List<String> userIds = requests.getContent().stream()
                .map(WithdrawalRequest::getUserId)
                .distinct()
                .toList();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        return requests.map(w -> toResponse(w, userMap.get(w.getUserId())));
    }

    /**
     * Admin cập nhật trạng thái yêu cầu rút tiền.
     * Khi duyệt (APPROVED) sẽ trừ số tiền tương ứng khỏi doanh thu của người dùng.
     */
    @Transactional
    public WithdrawalResponse updateStatus(String id, WithdrawalStatusUpdateRequest request) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_REQUEST_NOT_FOUND));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new AppException(ErrorCode.WITHDRAWAL_REQUEST_ALREADY_PROCESSED);
        }

        WithdrawalStatus newStatus;
        try {
            newStatus = WithdrawalStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        if (newStatus == WithdrawalStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        if (newStatus == WithdrawalStatus.APPROVED) {
            User user = userRepository.findById(withdrawal.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            double revenue = user.getRevenue() != null ? user.getRevenue() : 0.0;
            double amount = withdrawal.getAmount() != null ? withdrawal.getAmount() : 0.0;
            if (revenue < amount) {
                throw new AppException(ErrorCode.INSUFFICIENT_REVENUE);
            }
            user.setRevenue(revenue - amount);
            userRepository.save(user);
            log.info("Duyệt rút tiền - User: {}, đã trừ {} khỏi doanh thu", user.getId(), amount);
        }

        withdrawal.setStatus(newStatus);
        withdrawal.setAdminNote(request.getAdminNote());
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal = withdrawalRequestRepository.save(withdrawal);

        log.info("Cập nhật trạng thái yêu cầu rút tiền {} thành {}", id, newStatus);
        User owner = userRepository.findById(withdrawal.getUserId()).orElse(null);
        return toResponse(withdrawal, owner);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private WithdrawalResponse toResponse(WithdrawalRequest withdrawal, User user) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .userId(withdrawal.getUserId())
                .userEmail(user != null ? user.getEmail() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .amount(withdrawal.getAmount())
                .bankName(withdrawal.getBankName())
                .bankAccountHolderName(withdrawal.getBankAccountHolderName())
                .bankAccountNumber(withdrawal.getBankAccountNumber())
                .status(withdrawal.getStatus() != null ? withdrawal.getStatus().name() : null)
                .adminNote(withdrawal.getAdminNote())
                .createdAt(withdrawal.getCreatedAt())
                .processedAt(withdrawal.getProcessedAt())
                .build();
    }
}