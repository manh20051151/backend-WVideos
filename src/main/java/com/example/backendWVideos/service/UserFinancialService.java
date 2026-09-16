package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.FinancialHistoryResponse;
import com.example.backendWVideos.dto.response.UserFinancialInfoDTO;
import com.example.backendWVideos.entity.ProcessedTransaction;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.entity.VideoPurchase;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.ProcessedTransactionRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoPurchaseRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFinancialService {

    private final UserRepository userRepository;
    private final VideoPurchaseRepository videoPurchaseRepository;
    private final ProcessedTransactionRepository processedTransactionRepository;
    private final VideoRepository videoRepository;

    // Tỷ lệ doanh thu chia cho chủ video (%) - đồng bộ cấu hình với VideoService
    @Value("${app.revenue.creator-share-percent:70}")
    private double creatorSharePercent;

    private static final int EVENT_CAP = 200;          // Số bản ghi biến động trả về tối đa
    private static final int MONTHLY_WINDOW = 36;      // Số tháng thống kê tối đa trả về (frontend tự lọc khoảng)

    /**
     * Lịch sử tài chính cá nhân: gộp 3 nguồn dữ liệu thật đang có
     * (nạp Sepay, mua video, doanh thu từ video được sở hữu) thành một dòng thời gian biến động.
     */
    @Transactional(readOnly = true)
    public FinancialHistoryResponse getMyFinancialHistory() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<FinancialHistoryResponse.FinancialEvent> events = new ArrayList<>();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        double totalDeposited = 0;
        double deposited30d = 0;
        for (ProcessedTransaction t : processedTransactionRepository.findByUserIdOrderByProcessedAtDesc(user.getId())) {
            double amount = t.getAmount() != null ? t.getAmount() : 0.0;
            totalDeposited += amount;
            LocalDateTime at = t.getProcessedAt() != null ? t.getProcessedAt() : thirtyDaysAgo;
            if (at != null && at.isAfter(thirtyDaysAgo)) deposited30d += amount;
            // Không lộ nội dung chuyển khoản gốc (chứa số tài khoản người gửi và mã token NAPTIEN)
            events.add(FinancialHistoryResponse.FinancialEvent.builder()
                    .id(t.getId())
                    .type("TOPUP")
                    .title("Nạp tiền vào ví")
                    .description(t.getReferenceNumber() != null && !t.getReferenceNumber().isBlank()
                            ? "Chuyển khoản qua ngân hàng · Mã GD: " + t.getReferenceNumber()
                            : "Chuyển khoản qua ngân hàng")
                    .amount(amount)
                    .direction("IN")
                    .occurredAt(at)
                    .build());
        }

        double totalSpent = 0;
        double spent30d = 0;
        Set<String> videoIdsToLoad = new HashSet<>();
        List<VideoPurchase> myPurchases = videoPurchaseRepository.findByUserIdOrderByPurchasedAtDesc(user.getId());
        for (VideoPurchase p : myPurchases) {
            videoIdsToLoad.add(p.getVideoId());
        }
        List<VideoPurchase> incomingPurchases = videoPurchaseRepository.findPurchasesOfOwnerVideos(user.getId());
        for (VideoPurchase p : incomingPurchases) {
            videoIdsToLoad.add(p.getVideoId());
        }
        Map<String, String> videoTitles = loadVideoTitles(videoIdsToLoad);

        for (VideoPurchase p : myPurchases) {
            double amount = p.getPrice() != null ? p.getPrice().doubleValue() : 0.0;
            totalSpent += amount;
            if (p.getPurchasedAt() != null && p.getPurchasedAt().isAfter(thirtyDaysAgo)) spent30d += amount;
            events.add(FinancialHistoryResponse.FinancialEvent.builder()
                    .id(p.getId())
                    .type("VIDEO_PURCHASE")
                    .title("Mua video")
                    .description(videoTitles.getOrDefault(p.getVideoId(), "Video không còn tồn tại"))
                    .amount(amount)
                    .direction("OUT")
                    .occurredAt(p.getPurchasedAt())
                    .videoId(p.getVideoId())
                    .build());
        }

        double totalRevenue = 0;
        double revenue30d = 0;
        for (VideoPurchase p : incomingPurchases) {
            // Chủ video nhận theo đúng công thức chia sẻ doanh thu đã áp dụng lúc mua
            double gross = p.getPrice() != null ? p.getPrice().doubleValue() : 0.0;
            double earnings = gross * creatorSharePercent / 100.0;
            totalRevenue += earnings;
            if (p.getPurchasedAt() != null && p.getPurchasedAt().isAfter(thirtyDaysAgo)) revenue30d += earnings;
            events.add(FinancialHistoryResponse.FinancialEvent.builder()
                    .id(p.getId() + "-rev")
                    .type("CREATOR_REVENUE")
                    .title("Doanh thu người sáng tạo")
                    .description(videoTitles.getOrDefault(p.getVideoId(), "Video không còn tồn tại")
                            + " · chia " + (int) creatorSharePercent + "% từ giá " + formatVnd(gross))
                    .amount(earnings)
                    .direction("IN")
                    .occurredAt(p.getPurchasedAt())
                    .videoId(p.getVideoId())
                    .build());
        }

        events.sort(Comparator.comparing(
                FinancialHistoryResponse.FinancialEvent::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // Thống kê các tháng có dữ liệu (sớm nhất -> hiện tại, tối đa MONTHLY_WINDOW tháng) cho biểu đồ
        List<FinancialHistoryResponse.MonthlyStat> monthlyStats =
                buildMonthlyStats(events, YearMonth.now(), MONTHLY_WINDOW);

        double balance = user.getBalance() != null ? user.getBalance() : 0.0;
        double revenue = user.getRevenue() != null ? user.getRevenue() : 0.0;

        return FinancialHistoryResponse.builder()
                .balance(balance)
                .revenue(revenue)
                .totalDeposited(round(totalDeposited))
                .totalSpent(round(totalSpent))
                .deposited30d(round(deposited30d))
                .spent30d(round(spent30d))
                .revenue30d(round(revenue30d))
                .events(events.size() > EVENT_CAP ? events.subList(0, EVENT_CAP) : events)
                .monthlyStats(monthlyStats)
                .build();
    }

    private Map<String, String> loadVideoTitles(Set<String> videoIds) {
        if (videoIds.isEmpty()) return Map.of();
        Map<String, String> titles = new HashMap<>();
        for (Video v : videoRepository.findAllById(videoIds)) {
            titles.put(v.getId(), v.getTitle());
        }
        return titles;
    }

    private List<FinancialHistoryResponse.MonthlyStat> buildMonthlyStats(
            List<FinancialHistoryResponse.FinancialEvent> events, YearMonth current, int window) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        // Tháng có dữ liệu sớm nhất (guard Overflow khi tính chronological)
        YearMonth dataOldest = current;
        for (FinancialHistoryResponse.FinancialEvent e : events) {
            YearMonth ym = e.getOccurredAt() == null ? null : YearMonth.from(e.getOccurredAt());
            if (ym != null && ym.isBefore(dataOldest)) {
                dataOldest = ym;
            }
        }
        // Giới hạn tối đa `window` tháng gần nhất
        YearMonth floor = current.minusMonths((long) window - 1);
        YearMonth oldest = dataOldest.isBefore(floor) ? floor : dataOldest;

        Map<String, double[]> byMonth = new HashMap<>(); // [deposits, spending, earnings]
        for (FinancialHistoryResponse.FinancialEvent e : events) {
            if (e.getOccurredAt() == null) continue;
            YearMonth ym = YearMonth.from(e.getOccurredAt());
            if (ym.isBefore(oldest) || ym.isAfter(current)) continue;
            double[] row = byMonth.computeIfAbsent(ym.format(fmt), k -> new double[3]);
            double amount = e.getAmount() != null ? e.getAmount() : 0.0;
            switch (e.getType()) {
                case "TOPUP" -> row[0] += amount;
                case "VIDEO_PURCHASE" -> row[1] += amount;
                case "CREATOR_REVENUE" -> row[2] += amount;
                default -> { }
            }
        }
        int months = (int) java.time.temporal.ChronoUnit.MONTHS.between(oldest, current) + 1;
        months = Math.max(1, Math.min(months, window));
        List<FinancialHistoryResponse.MonthlyStat> stats = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            String key = oldest.plusMonths(i).format(fmt);
            double[] row = byMonth.getOrDefault(key, new double[3]);
            stats.add(FinancialHistoryResponse.MonthlyStat.builder()
                    .month(key)
                    .deposits(round(row[0]))
                    .spending(round(row[1]))
                    .earnings(round(row[2]))
                    .build());
        }
        return stats;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String formatVnd(double v) {
        return String.format("%,.0fđ", v);
    }

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
