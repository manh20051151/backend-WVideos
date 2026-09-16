package com.example.backendWVideos.entity;

import com.example.backendWVideos.enums.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(nullable = false)
    Double amount;

    @Column(name = "bank_name", length = 100)
    String bankName;

    @Column(name = "bank_account_holder_name", length = 100)
    String bankAccountHolderName;

    @Column(name = "bank_account_number", length = 20)
    String bankAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    String adminNote;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "processed_at")
    LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = WithdrawalStatus.PENDING;
        }
    }
}