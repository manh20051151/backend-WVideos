package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.ReportReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportReasonRepository extends JpaRepository<ReportReason, Long> {

    // Danh sách lý do đang kích hoạt cho modal báo cáo (theo thứ tự hiển thị)
    List<ReportReason> findByIsActiveTrueOrderBySortOrderAscIdAsc();

    // Danh sách tất cả lý do cho trang admin
    Page<ReportReason> findAllByOrderBySortOrderAscIdAsc(Pageable pageable);

    // Tìm theo code (để validate khi user gửi báo cáo)
    Optional<ReportReason> findByCodeIgnoreCaseAndIsActiveTrue(String code);

    boolean existsByCodeIgnoreCase(String code);
}
