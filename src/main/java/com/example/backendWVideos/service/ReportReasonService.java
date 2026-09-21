package com.example.backendWVideos.service;

import com.example.backendWVideos.dto.response.ReportReasonResponse;
import com.example.backendWVideos.entity.ReportReason;
import com.example.backendWVideos.exception.AppException;
import com.example.backendWVideos.exception.ErrorCode;
import com.example.backendWVideos.repository.ReportReasonRepository;
import com.example.backendWVideos.repository.VideoReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Quản lý danh sách lý do báo cáo video (admin CRUD).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportReasonService {

    private final ReportReasonRepository reportReasonRepository;
    private final VideoReportRepository videoReportRepository;

    /**
     * Danh sách lý do đang kích hoạt (public - dùng cho modal báo cáo).
     */
    @Transactional(readOnly = true)
    public List<ReportReasonResponse> getActiveReasons() {
        return reportReasonRepository.findByIsActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tất cả lý do (admin - kể cả đã ẩn).
     */
    @Transactional(readOnly = true)
    public Page<ReportReasonResponse> getAllReasons(Pageable pageable) {
        return reportReasonRepository.findAllByOrderBySortOrderAscIdAsc(pageable).map(this::toResponse);
    }

    /**
     * Tạo lý do mới. Code phải unique.
     */
    @Transactional
    public ReportReasonResponse createReason(com.example.backendWVideos.dto.request.ReportReasonRequest request) {
        String code = request.getCode() == null ? null : request.getCode().trim().toUpperCase();
        if (code == null || code.isBlank()) {
            throw new AppException(ErrorCode.INVALID_DATA);
        }
        if (reportReasonRepository.existsByCodeIgnoreCase(code)) {
            throw new AppException(ErrorCode.REPORT_REASON_CODE_EXISTED);
        }

        ReportReason reason = ReportReason.builder()
                .code(code)
                .label(request.getLabel().trim())
                .icon(request.getIcon())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .createdAt(LocalDateTime.now())
                .build();
        ReportReason saved = reportReasonRepository.save(reason);
        log.info("✅ Đã tạo lý do báo cáo mới: {} ({})", saved.getLabel(), saved.getCode());
        return toResponse(saved);
    }

    /**
     * Cập nhật nhãn/icon/thứ tự/trạng thái. Không cho đổi code (đã tham chiếu trong báo cáo).
     */
    @Transactional
    public ReportReasonResponse updateReason(Long id, com.example.backendWVideos.dto.request.ReportReasonRequest request) {
        ReportReason reason = reportReasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_REASON_NOT_FOUND));

        if (request.getLabel() != null && !request.getLabel().isBlank()) {
            reason.setLabel(request.getLabel().trim());
        }
        reason.setIcon(request.getIcon());
        if (request.getSortOrder() != null) {
            reason.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            reason.setIsActive(request.getIsActive());
        }
        reason.setUpdatedAt(LocalDateTime.now());

        ReportReason saved = reportReasonRepository.save(reason);
        log.info("🔄 Đã cập nhật lý do báo cáo: {} ({})", saved.getLabel(), saved.getCode());
        return toResponse(saved);
    }

    /**
     * Xóa lý do - chặn nếu đã có báo cáo dùng lý do này (đề xuất ẩn thay vì xóa).
     */
    @Transactional
    public void deleteReason(Long id) {
        ReportReason reason = reportReasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_REASON_NOT_FOUND));
        if (videoReportRepository.existsByReasonIgnoreCase(reason.getCode())) {
            throw new AppException(ErrorCode.REPORT_REASON_IN_USE);
        }
        reportReasonRepository.delete(reason);
        log.info("🗑️ Đã xóa lý do báo cáo: {} ({})", reason.getLabel(), reason.getCode());
    }

    private ReportReasonResponse toResponse(ReportReason reason) {
        return ReportReasonResponse.builder()
                .id(reason.getId())
                .code(reason.getCode())
                .label(reason.getLabel())
                .icon(reason.getIcon())
                .sortOrder(reason.getSortOrder())
                .isActive(reason.getIsActive())
                .createdAt(reason.getCreatedAt())
                .updatedAt(reason.getUpdatedAt())
                .build();
    }
}
