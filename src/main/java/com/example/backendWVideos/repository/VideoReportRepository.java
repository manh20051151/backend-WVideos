package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.VideoReport;
import com.example.backendWVideos.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoReportRepository extends JpaRepository<VideoReport, Long> {

    // Kiểm tra user đã báo cáo video này chưa
    boolean existsByReporterUserIdAndVideoId(String reporterUserId, String videoId);

    // Kiểm tra lý do đã được dùng trong báo cáo chưa (chặn xóa lý do đang dùng)
    boolean existsByReasonIgnoreCase(String reason);

    Optional<VideoReport> findByReporterUserIdAndVideoId(String reporterUserId, String videoId);

    // Danh sách báo cáo cho admin (lọc theo trạng thái, mới nhất trước)
    @Query("SELECT r FROM VideoReport r WHERE (:status IS NULL OR r.status = :status) " +
           "ORDER BY r.createdAt DESC")
    Page<VideoReport> findReports(@Param("status") ReportStatus status, Pageable pageable);

    // Báo cáo của một user (trang "Báo cáo của tôi")
    Page<VideoReport> findByReporterUserIdOrderByCreatedAtDesc(String reporterUserId, Pageable pageable);

    // Đếm số báo cáo theo trạng thái (cho badge tab admin)
    @Query("SELECT r.status, COUNT(r) FROM VideoReport r GROUP BY r.status")
    java.util.List<Object[]> countByStatus();
}
