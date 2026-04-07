package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.SepayResponseDTO;
import com.example.backendWVideos.dto.response.SepayTransactionDTO;
import com.example.backendWVideos.dto.response.TransactionCheckResultDTO;
import com.example.backendWVideos.dto.response.UserFinancialInfoDTO;
import com.example.backendWVideos.entity.ProcessedTransaction;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.repository.ProcessedTransactionRepository;
import com.example.backendWVideos.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SepayService {

    private final RestTemplate restTemplate;
    private final UserFinancialService userFinancialService;
    private final ProcessedTransactionRepository processedTransactionRepository;
    private final UserRepository userRepository;

    @Value("${app.sepay.api-url:https://my.sepay.vn/userapi/transactions/list}")
    private String sepayApiUrl;

    @Value("${app.sepay.account-number:0375000169}")
    private String accountNumber;

    @Value("${app.sepay.api-token:}")
    private String apiToken;

    private static final Pattern USER_ID_PATTERN = Pattern.compile(".*NAPTIEN([A-Za-z0-9]{8})([a-fA-F0-9]{32}).*");

    public SepayService(RestTemplate restTemplate, UserFinancialService userFinancialService,
                       ProcessedTransactionRepository processedTransactionRepository, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.userFinancialService = userFinancialService;
        this.processedTransactionRepository = processedTransactionRepository;
        this.userRepository = userRepository;
    }

    public SepayResponseDTO getTransactions(int limit) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = sepayApiUrl + "?account_number=" + accountNumber + "&limit=" + limit;

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            SepayResponseDTO result = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(response.getBody(), SepayResponseDTO.class);
            
            return result;

        } catch (Exception e) {
            return SepayResponseDTO.builder()
                .status(500)
                .error("API call failed: " + e.getMessage())
                .build();
        }
    }

    public TransactionCheckResultDTO checkTransactionByDescription(String description, int limit) {
        SepayResponseDTO sepayResponse = getTransactions(limit);

        if (sepayResponse == null || sepayResponse.getStatus() != 200) {
            return TransactionCheckResultDTO.builder()
                .found(false)
                .status("ERROR")
                .message("Failed to fetch transactions from SePay API")
                .searchDescription(description)
                .totalTransactionsChecked(0)
                .build();
        }

        List<SepayTransactionDTO> transactions = sepayResponse.getTransactions();
        if (transactions == null || transactions.isEmpty()) {
            return TransactionCheckResultDTO.builder()
                .found(false)
                .status("NO_TRANSACTIONS")
                .message("No transactions found")
                .searchDescription(description)
                .totalTransactionsChecked(0)
                .build();
        }

        for (SepayTransactionDTO transaction : transactions) {
            if (transaction.getTransactionContent() != null) {
                String naptienPart = extractNaptienPart(transaction.getTransactionContent());
                
                if (naptienPart != null && naptienPart.trim().equalsIgnoreCase(description.trim())) {
                    processDepositTransaction(transaction);
                    
                    return TransactionCheckResultDTO.builder()
                        .found(true)
                        .status("FOUND")
                        .message("Transaction found with NAPTIEN part match. Balance updated if applicable.")
                        .matchedTransaction(transaction)
                        .searchDescription(description)
                        .totalTransactionsChecked(transactions.size())
                        .build();
                }
            }
        }

        return TransactionCheckResultDTO.builder()
            .found(false)
            .status("NOT_FOUND")
            .message("No transaction found with NAPTIEN part matching description")
            .searchDescription(description)
            .totalTransactionsChecked(transactions.size())
            .build();
    }

    public TransactionCheckResultDTO checkTransactionByAmountAndDescription(String description, Double amount, int limit) {
        String transactionId = "";
        if (description != null && description.startsWith("NAPTIEN")) {
            String cleaned = description.replace(" ", "").replace("+", "");
            if (cleaned.length() > 8) {
                transactionId = cleaned.substring(8, 16);
            }
        }

        SepayResponseDTO sepayResponse = getTransactions(limit);

        if (sepayResponse == null || sepayResponse.getStatus() != 200) {
            return TransactionCheckResultDTO.builder()
                .found(false)
                .status("ERROR")
                .message("Failed to fetch transactions from SePay API")
                .searchDescription(description)
                .totalTransactionsChecked(0)
                .build();
        }

        List<SepayTransactionDTO> transactions = sepayResponse.getTransactions();
        
        if (transactions == null || transactions.isEmpty()) {
            return TransactionCheckResultDTO.builder()
                .found(false)
                .status("NO_TRANSACTIONS")
                .message("No transactions found")
                .searchDescription(description)
                .totalTransactionsChecked(0)
                .build();
        }

        for (SepayTransactionDTO transaction : transactions) {
            if (transaction.getTransactionContent() != null) {
                String content = transaction.getTransactionContent().toUpperCase().replace(" ", "").replace("+", "");
                String searchTransactionId = transactionId.toUpperCase();
                
                boolean hasNapTien = content.contains("NAPTIEN");
                boolean hasTransactionId = content.contains(searchTransactionId);
                
                if (hasNapTien && hasTransactionId) {
                    try {
                        processDepositTransaction(transaction);
                    } catch (Exception e) {
                        log.error("Error during deposit processing for transaction {}: {}", transaction.getId(), e.getMessage(), e);
                    }
                    
                    return TransactionCheckResultDTO.builder()
                        .found(true)
                        .status("FOUND")
                        .message("Transaction found with NAPTIEN part match. Balance updated if applicable.")
                        .matchedTransaction(transaction)
                        .searchDescription(description)
                        .totalTransactionsChecked(transactions.size())
                        .build();
                }
            }
        }

        return TransactionCheckResultDTO.builder()
            .found(false)
            .status("NOT_FOUND")
            .message("No transaction found with NAPTIEN part matching description and specified amount")
            .searchDescription(description)
            .totalTransactionsChecked(transactions.size())
            .build();
    }

    private String extractUserIdFromDescription(String transactionContent) {
        if (transactionContent == null) {
            return null;
        }
        
        String upperContent = transactionContent.toUpperCase().replace(" ", "").replace("+", "");
        
        Pattern pattern = Pattern.compile("NAPTIEN([A-Z0-9]{8})([A-F0-9]{32})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(upperContent);
        
        if (matcher.find()) {
            String userIdWithoutHyphens = matcher.group(2);
            return formatUUID(userIdWithoutHyphens.toLowerCase());
        }
        
        Pattern fallbackPattern = Pattern.compile("NAPTIEN[A-Z0-9]{0,20}([A-F0-9]{32})", Pattern.CASE_INSENSITIVE);
        Matcher fallbackMatcher = fallbackPattern.matcher(upperContent);
        
        if (fallbackMatcher.find()) {
            String userIdWithoutHyphens = fallbackMatcher.group(1);
            return formatUUID(userIdWithoutHyphens.toLowerCase());
        }
        
        return null;
    }

    private String extractNaptienPart(String transactionContent) {
        if (transactionContent == null) {
            return null;
        }
        
        int naptienIndex = transactionContent.toUpperCase().indexOf("NAPTIEN");
        if (naptienIndex != -1) {
            return transactionContent.substring(naptienIndex).trim();
        }
        
        return null;
    }

    private String formatUUID(String uuidWithoutHyphens) {
        if (uuidWithoutHyphens == null || uuidWithoutHyphens.length() != 32) {
            return null;
        }
        
        return String.format("%s-%s-%s-%s-%s",
                uuidWithoutHyphens.substring(0, 8),
                uuidWithoutHyphens.substring(8, 12),
                uuidWithoutHyphens.substring(12, 16),
                uuidWithoutHyphens.substring(16, 20),
                uuidWithoutHyphens.substring(20, 32)
        );
    }

    private void processDepositTransaction(SepayTransactionDTO transaction) {
        boolean alreadyProcessed = processedTransactionRepository.existsByTransactionId(transaction.getId());
        
        if (alreadyProcessed) {
            return;
        }
        
        String transactionContent = transaction.getTransactionContent();
        if (transactionContent == null || !transactionContent.toUpperCase().contains("NAPTIEN")) {
            return;
        }
        
        String userId = extractUserIdFromDescription(transactionContent);
        
        if (userId == null) {
            return;
        }
        
        Double amount = 0.0;
        try {
            if (transaction.getAmountIn() != null && !transaction.getAmountIn().isEmpty()) {
                amount = Double.parseDouble(transaction.getAmountIn());
            }
        } catch (NumberFormatException e) {
            return;
        }
        
        if (amount <= 0) {
            return;
        }
        
        userFinancialService.updateUserBalance(userId, amount, "ADD");
        
        ProcessedTransaction processedTxn = ProcessedTransaction.builder()
                .transactionId(transaction.getId())
                .userId(userId)
                .amount(amount)
                .transactionContent(transaction.getTransactionContent())
                .sepayTransactionDate(transaction.getTransactionDate())
                .referenceNumber(transaction.getReferenceNumber())
                .build();
        
        processedTransactionRepository.save(processedTxn);
    }
}
