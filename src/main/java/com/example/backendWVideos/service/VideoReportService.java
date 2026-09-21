package com.example.backendWVideos.service;

import com.example.backendWVideos.config.CdnProperties;
import com.example.backendWVideos.dto.response.VideoReportResponse;
import com.example.backendWVideos.entity.User;
import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.entity.VideoReport;
import com.example.backendWVideos.enums.ReportStatus;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.ReportReasonRepository;
import com.example.backendWVideos.repository.UserRepository;
import com.example.backendWVideos.repository.VideoReportRepository;
import com.example.backendWVideos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Xử lý báo cáo vi phạm video: user gửi báo cáo, admin duyệt/loại bỏ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoReportService {

    private final VideoReportRepository videoReportRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final ReportReasonRepository reportReasonRepository;
    private final CdnProperties cdnProperties;
    private final NotificationService notificationService;

    /**
     * User gửi báo cáo vi phạm cho video. Mỗi user chỉ báo cáo 1 lần cho mỗi video.
     */
    @Transactional
    public VideoReportResponse createReport(String reporterEmail, String videoId,
                                            com.example.backendWVideos.dto.request.VideoReportRequest request) {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        // Không cho báo cáo video của chính mình
        if (reporter.getId().equals(video.getUser() != null ? video.getUser().getId() : null)) {
            throw new AppException(ErrorCode.CANNOT_REPORT_OWN_VIDEO);
        }

        // Validate lý do báo cáo: phải tồn tại và đang được kích hoạt
        reportReasonRepository.findByCodeIgnoreCaseAndIsActiveTrue(request.getReason())
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_REASON_INVALID));

        if (videoReportRepository.existsByReporterUserIdAndVideoId(reporter.getId(), videoId)) {
            throw new AppException(ErrorCode.VIDEO_ALREADY_REPORTED);
        }

        VideoReport report = VideoReport.builder()
                .videoId(videoId)
                .reporterUserId(reporter.getId())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        VideoReport saved = videoReportRepository.save(report);
        log.info("🚩 User {} đã báo cáo video {} với lý do {}", reporter.getEmail(), videoId, request.getReason());
        return toResponse(saved, video, reporter);
    }

    /**
     * Danh sách báo cáo cho admin (lọc theo trạng thái), kèm thông tin video + user báo cáo.
     */
    @Transactional(readOnly = true)
    public Page<VideoReportResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<VideoReport> reports = videoReportRepository.findReports(status, pageable);

        // Batch load video + user để tránh N+1
        Set<String> videoIds = reports.getContent().stream()
                .map(VideoReport::getVideoId).collect(Collectors.toSet());
        Set<String> userIds = reports.getContent().stream()
                .map(VideoReport::getReporterUserId).collect(Collectors.toSet());
        Map<String, Video> videoMap = videoIds.isEmpty() ? Map.of()
                : videoRepository.findAllById(videoIds).stream()
                        .collect(Collectors.toMap(Video::getId, Function.identity()));
        Map<String, User> userMap = userIds.isEmpty() ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        return reports.map(r -> toResponse(r, videoMap.get(r.getVideoId()), userMap.get(r.getReporterUserId())));
    }

    /**
     * Đếm số báo cáo theo trạng thái (cho badge tab admin).
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (ReportStatus s : ReportStatus.values()) {
            counts.put(s.name(), 0L);
        }
        videoReportRepository.countByStatus()
                .forEach(row -> counts.put(((ReportStatus) row[0]).name(), (Long) row[1]));
        return counts;
    }

    /**
     * Danh sách báo cáo của user hiện tại (trang "Báo cáo của tôi" trong profile).
     */
    @Transactional(readOnly = true)
    public Page<VideoReportResponse> getMyReports(String reporterEmail, Pageable pageable) {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Page<VideoReport> reports = videoReportRepository
                .findByReporterUserIdOrderByCreatedAtDesc(reporter.getId(), pageable);

        // Batch load video để hiển thị tiêu đề/thumbnail
        Set<String> videoIds = reports.getContent().stream()
                .map(VideoReport::getVideoId).collect(Collectors.toSet());
        Map<String, Video> videoMap = videoIds.isEmpty() ? Map.of()
                : videoRepository.findAllById(videoIds).stream()
                        .collect(Collectors.toMap(Video::getId, Function.identity()));

        return reports.map(r -> toResponse(r, videoMap.get(r.getVideoId()), reporter));
    }

    /**
     * User rút lại báo cáo đang chờ xử lý (chỉ được rút báo cáo của chính mình).
     */
    @Transactional
    public void withdrawReport(String reporterEmail, Long reportId) {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        VideoReport report = videoReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_REPORT_NOT_FOUND));

        if (!reporter.getId().equals(report.getReporterUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }
        videoReportRepository.delete(report);
        log.info("↩️ User {} đã rút báo cáo #{}", reporter.getEmail(), reportId);
    }

    /**
     * Admin xử lý báo cáo: đánh dấu đã xử lý hoặc bỏ qua.
     */
    @Transactional
    public VideoReportResponse updateStatus(String adminEmail, Long reportId,
                                            com.example.backendWVideos.dto.request.VideoReportUpdateRequest request) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (request.getStatus() != ReportStatus.RESOLVED && request.getStatus() != ReportStatus.DISMISSED) {
            throw new AppException(ErrorCode.INVALID_DATA);
        }
        // Bỏ qua báo cáo phải kèm lý do để người báo cáo hiểu vì sao
        if (request.getStatus() == ReportStatus.DISMISSED
                && (request.getAdminNote() == null || request.getAdminNote().isBlank())) {
            throw new AppException(ErrorCode.REPORT_DISMISS_NOTE_REQUIRED);
        }

        VideoReport report = videoReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_REPORT_NOT_FOUND));

        report.setStatus(request.getStatus());
        report.setAdminNote(request.getAdminNote());
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(admin.getId());
        VideoReport saved = videoReportRepository.save(report);
        log.info("✅ Admin {} đã xử lý báo cáo #{} -> {}", adminEmail, reportId, request.getStatus());

        Video video = videoRepository.findById(saved.getVideoId()).orElse(null);
        User reporter = userRepository.findById(saved.getReporterUserId()).orElse(null);

        // Thông báo realtime cho người báo cáo biết kết quả xử lý
        if (reporter != null) {
            try {
                notificationService.notifyReportProcessed(
                        reporter.getId(),
                        request.getStatus() == ReportStatus.RESOLVED,
                        saved.getVideoId(),
                        video != null ? video.getTitle() : null,
                        request.getAdminNote(),
                        video != null ? cdnProperties.convertThumbnailUrl(video.getThumbnailUrl()) : null);
            } catch (Exception e) {
                log.warn("Không gửi được thông báo xử lý báo cáo cho user {}: {}", reporter.getId(), e.getMessage());
            }
        }

        return toResponse(saved, video, reporter);
    }

    private VideoReportResponse toResponse(VideoReport report, Video video, User reporter) {
        return VideoReportResponse.builder()
                .id(report.getId())
                .videoId(report.getVideoId())
                .videoTitle(video != null ? video.getTitle() : null)
                .videoSlug(video != null ? video.getSlug() : null)
                .videoThumbnailUrl(video != null ? cdnProperties.convertThumbnailUrl(video.getThumbnailUrl()) : null)
                .reporterName(reporter != null ? reporter.getFullName() : null)
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .adminNote(report.getAdminNote())
                .build();
    }
}
