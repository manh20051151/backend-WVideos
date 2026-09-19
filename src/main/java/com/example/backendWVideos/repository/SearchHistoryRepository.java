package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    // Lịch sử gần đây nhất của user (dùng cho gợi ý khi focus ô tìm kiếm)
    List<SearchHistory> findTop20ByUserIdOrderBySearchedAtDesc(String userId);

    Optional<SearchHistory> findByUserIdAndQuery(String userId, String query);

    // Xóa 1 mục lịch sử - đảm bảo chỉ xóa được của chính mình
    @Modifying
    @Query("DELETE FROM SearchHistory sh WHERE sh.id = :id AND sh.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    // Xóa toàn bộ lịch sử của user
    @Modifying
    @Query("DELETE FROM SearchHistory sh WHERE sh.userId = :userId")
    int deleteAllByUserId(@Param("userId") String userId);
}
