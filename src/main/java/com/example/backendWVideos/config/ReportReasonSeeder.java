package com.example.backendWVideos.config;

import com.example.backendWVideos.entity.ReportReason;
import com.example.backendWVideos.repository.ReportReasonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seed dữ liệu mặc định cho lý do báo cáo video (chỉ chạy khi bảng report_reasons trống).
 * Icon là SVG path data - frontend render trực tiếp trong thẻ <svg>.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportReasonSeeder implements ApplicationRunner {

    private final ReportReasonRepository reportReasonRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (reportReasonRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<ReportReason> defaults = List.of(
                build("SPAM", "Spam / quảng cáo lừa đảo",
                        "M12 2a10 10 0 100 20 10 10 0 000-20zm0 5v5l4 2", 1, now),
                build("INAPPROPRIATE", "Nội dung không phù hợp (18+)",
                        "M12 9v2m0 4h.01M5 19h14a2 2 0 001.84-2.75L13.74 4a2 2 0 00-3.48 0L3.16 16.25A2 2 0 005 19z", 2, now),
                build("VIOLENT", "Nội dung bạo lực, gây hại",
                        "M12 21a9 9 0 01-9-9c0-2.92 1.9-5.42 4.53-6.3M21 12a9 9 0 01-9 9m9-9c0 2.92-1.9 5.42-4.53 6.3M12 3v4m-4 5h8v5H8v-5z", 3, now),
                build("COPYRIGHT", "Vi phạm bản quyền",
                        "M9 12h6m-6 4h6M9 8h6M5 3h14a2 2 0 012 2v14a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2z", 4, now),
                build("PRIVACY", "Xâm phạm quyền riêng tư",
                        "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z", 5, now),
                build("OTHER", "Lý do khác",
                        "M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z", 6, now)
        );
        reportReasonRepository.saveAll(defaults);
        log.info("✅ Đã seed {} lý do báo cáo mặc định", defaults.size());
    }

    private ReportReason build(String code, String label, String icon, int sortOrder, LocalDateTime now) {
        return ReportReason.builder()
                .code(code)
                .label(label)
                .icon(icon)
                .sortOrder(sortOrder)
                .isActive(true)
                .createdAt(now)
                .build();
    }
}
