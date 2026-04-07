package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "transaction_id", nullable = false)
    String transactionId;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(nullable = false)
    Double amount;

    @Column(name = "transaction_content", columnDefinition = "TEXT")
    String transactionContent;

    @Column(name = "sepay_transaction_date")
    String sepayTransactionDate;

    @Column(name = "reference_number")
    String referenceNumber;

    @Column(name = "processed_at")
    LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
}
