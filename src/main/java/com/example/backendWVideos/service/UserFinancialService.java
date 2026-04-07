package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.UserFinancialInfoDTO;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFinancialService {

    private final UserRepository userRepository;

    public UserFinancialInfoDTO getUserFinancialInfo(String userId) {
        log.info("Getting financial info for user: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("User not found with id: {}", userId);
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        User user = userOpt.get();
        return UserFinancialInfoDTO.builder()
                .userId(user.getId())
                .balance(user.getBalance() != null ? user.getBalance() : 0.0)
                .revenue(user.getRevenue() != null ? user.getRevenue() : 0.0)
                .message("Lấy thông tin tài chính thành công")
                .build();
    }

    @Transactional
    public UserFinancialInfoDTO updateUserBalance(String userId, Double amount, String operation) {
        log.info("Updating balance for user: {}, amount: {}, operation: {}", userId, amount, operation);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("User not found with id: {}", userId);
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        User user = userOpt.get();
        Double currentBalance = user.getBalance() != null ? user.getBalance() : 0.0;
        
        if ("ADD".equalsIgnoreCase(operation)) {
            user.setBalance(currentBalance + amount);
            log.info("Added {} to balance. New balance: {}", amount, user.getBalance());
        } else if ("SUBTRACT".equalsIgnoreCase(operation)) {
            if (currentBalance < amount) {
                log.error("Insufficient balance for user: {}. Current: {}, Requested: {}", 
                        userId, currentBalance, amount);
                throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
            }
            user.setBalance(currentBalance - amount);
            log.info("Subtracted {} from balance. New balance: {}", amount, user.getBalance());
        } else {
            log.error("Invalid operation: {}", operation);
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }
        
        userRepository.save(user);
        
        return UserFinancialInfoDTO.builder()
                .userId(user.getId())
                .balance(user.getBalance())
                .revenue(user.getRevenue() != null ? user.getRevenue() : 0.0)
                .message("Cập nhật số dư thành công")
                .build();
    }

    @Transactional
    public UserFinancialInfoDTO updateUserRevenue(String userId, Double amount) {
        log.info("Updating revenue for user: {}, amount: {}", userId, amount);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("User not found with id: {}", userId);
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        User user = userOpt.get();
        Double currentRevenue = user.getRevenue() != null ? user.getRevenue() : 0.0;
        
        user.setRevenue(currentRevenue + amount);
        log.info("Added {} to revenue. New revenue: {}", amount, user.getRevenue());
        
        userRepository.save(user);
        
        return UserFinancialInfoDTO.builder()
                .userId(user.getId())
                .balance(user.getBalance() != null ? user.getBalance() : 0.0)
                .revenue(user.getRevenue())
                .message("Cập nhật doanh thu thành công")
                .build();
    }
}
