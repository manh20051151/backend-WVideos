package com.example.backendWVideos.controller;

import com.example.backendWVideos.dto.request.ApiResponse;
import com.example.backendWVideos.dto.response.SepayResponseDTO;
import com.example.backendWVideos.dto.response.TransactionCheckResultDTO;
import com.example.backendWVideos.dto.response.UserFinancialInfoDTO;
import com.example.backendWVideos.service.SepayService;
import com.example.backendWVideos.service.UserFinancialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sepay")
@RequiredArgsConstructor
public class SepayController {

    private final SepayService sepayService;
    private final UserFinancialService userFinancialService;

    @PostMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SepayResponseDTO> getTransactions(
            @RequestBody Map<String, Object> request
    ) {
        int limit = request.containsKey("limit") ? 
            Integer.parseInt(request.get("limit").toString()) : 20;
        
        log.info("Getting transactions with limit: {}", limit);
        
        SepayResponseDTO transactions = sepayService.getTransactions(limit);
        return ApiResponse.<SepayResponseDTO>builder()
                .code(1000)
                .message("Lấy danh sách giao dịch thành công")
                .result(transactions)
                .build();
    }

    @PostMapping("/check-transaction-with-amount")
    @PreAuthorize("hasRole('GUEST') or hasRole('ADMIN')")
    public ApiResponse<String> checkTransactionByAmountAndDescription(
            @RequestBody Map<String, Object> request
    ) {
        String description = request.get("description").toString();
        Double amount = Double.valueOf(request.get("amount").toString());
        int limit = request.containsKey("limit") ? 
            Integer.parseInt(request.get("limit").toString()) : 20;
            
        log.info("Checking transaction with description: {}, amount: {}, limit: {}", description, amount, limit);
        
        TransactionCheckResultDTO result = sepayService.checkTransactionByAmountAndDescription(description, amount, limit);
        
        String message = result.isFound() ? 
            "Tìm thấy giao dịch khớp với mô tả và số tiền" : 
            "Không tìm thấy giao dịch khớp với mô tả và số tiền";
            
        return ApiResponse.<String>builder()
                .code(1000)
                .message(message)
                .result(result.getStatus())
                .build();
    }

    @PostMapping("/check-status")
    @PreAuthorize("hasRole('GUEST') or hasRole('ADMIN')")
    public ApiResponse<String> checkTransactionStatus(
            @RequestBody Map<String, Object> request
    ) {
        String description = request.get("description").toString();
        int limit = request.containsKey("limit") ? 
            Integer.parseInt(request.get("limit").toString()) : 20;
            
        log.info("Checking transaction status with description: {}", description);
        
        TransactionCheckResultDTO result = sepayService.checkTransactionByDescription(description, limit);
        
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Kiểm tra trạng thái giao dịch thành công")
                .result(result.getStatus())
                .build();
    }

    @PostMapping("/process-deposit")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TransactionCheckResultDTO> processDepositTransaction(
            @RequestBody Map<String, Object> request
    ) {
        String description = request.get("description").toString();
        int limit = request.containsKey("limit") ? 
            Integer.parseInt(request.get("limit").toString()) : 20;
            
        log.info("Processing deposit transaction with description: {}", description);
        
        TransactionCheckResultDTO result = sepayService.checkTransactionByDescription(description, limit);
        
        String message;
        if (result.isFound()) {
            message = "Giao dịch nạp tiền đã được xử lý thành công";
        } else {
            message = "Không tìm thấy giao dịch nạp tiền khớp";
        }
            
        return ApiResponse.<TransactionCheckResultDTO>builder()
                .code(1000)
                .message(message)
                .result(result)
                .build();
    }

    @PostMapping("/test-pattern")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> testPatternMatching(
            @RequestBody Map<String, Object> request
    ) {
        String transactionContent = request.get("transaction_content").toString();
        
        log.info("Testing pattern matching for: {}", transactionContent);
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*NAPTIEN([A-Za-z0-9]{8})([a-f0-9]{32}).*");
        java.util.regex.Matcher matcher = pattern.matcher(transactionContent.toUpperCase());
        
        Map<String, Object> result = new HashMap<>();
        result.put("input", transactionContent);
        result.put("upperCase", transactionContent.toUpperCase());
        
        if (matcher.find()) {
            String randomCode = matcher.group(1);
            String userIdWithoutHyphens = matcher.group(2);
            String formattedUserId = String.format("%s-%s-%s-%s-%s",
                    userIdWithoutHyphens.substring(0, 8).toLowerCase(),
                    userIdWithoutHyphens.substring(8, 12).toLowerCase(),
                    userIdWithoutHyphens.substring(12, 16).toLowerCase(),
                    userIdWithoutHyphens.substring(16, 20).toLowerCase(),
                    userIdWithoutHyphens.substring(20, 32).toLowerCase()
            );
            
            result.put("matched", true);
            result.put("randomCode", randomCode);
            result.put("userIdWithoutHyphens", userIdWithoutHyphens);
            result.put("formattedUserId", formattedUserId);
        } else {
            result.put("matched", false);
            result.put("error", "Pattern did not match");
        }
        
        return ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .message("Pattern test completed")
                .result(result)
                .build();
    }
}
