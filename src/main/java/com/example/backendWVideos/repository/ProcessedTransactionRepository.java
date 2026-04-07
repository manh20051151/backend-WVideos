package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.ProcessedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessedTransactionRepository extends JpaRepository<ProcessedTransaction, String> {
    boolean existsByTransactionId(String transactionId);
    List<ProcessedTransaction> findAllByOrderByProcessedAtDesc();
}
