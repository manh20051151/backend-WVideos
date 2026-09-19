package com.example.backendWVideos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lịch sử tìm kiếm của user (giống YouTube): mỗi từ khóa duy nhất 1 dòng,
 * tìm lại từ khóa cũ thì cập nhật thời gian và tăng số lần tìm.
 */
@Entity
@Table(
        name = "search_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_search_history_user_query",
                columnNames = {"user_id", "query"}
        ),
        indexes = @Index(name = "idx_search_history_user", columnList = "user_id")
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId; // ID user đã đăng nhập

    @Column(name = "query", nullable = false, length = 255)
    private String query; // Từ khóa đã tìm

    @Column(name = "search_count", nullable = false)
    private Long searchCount = 1L; // Số lần tìm từ khóa này

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;
}
