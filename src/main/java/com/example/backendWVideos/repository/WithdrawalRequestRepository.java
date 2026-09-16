package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.WithdrawalRequest;
import com.example.backendWVideos.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, String> {

    // Chặn tạo yêu cầu mới khi vẫn còn yêu cầu chưa xử lý
    boolean existsByUserIdAndStatus(String userId, WithdrawalStatus status);

    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<WithdrawalRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status, Pageable pageable);
}