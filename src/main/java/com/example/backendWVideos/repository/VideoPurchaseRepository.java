package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.VideoPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoPurchaseRepository extends JpaRepository<VideoPurchase, String> {

    List<VideoPurchase> findByUserIdAndVideoId(String userId, String videoId);

    boolean existsByUserIdAndVideoId(String userId, String videoId);

    Page<VideoPurchase> findByUserIdOrderByPurchasedAtDesc(String userId, Pageable pageable);

    // Toàn bộ lượt mua video của user (không phân trang - phục vụ thống kê tài chính cá nhân)
    List<VideoPurchase> findByUserIdOrderByPurchasedAtDesc(String userId);

    // Doanh thu: mọi lượt mua video thuộc sở hữu của ownerId
    @Query("SELECT vp FROM VideoPurchase vp WHERE vp.videoId IN " +
           "(SELECT v.id FROM Video v WHERE v.user.id = :ownerId) " +
           "ORDER BY vp.purchasedAt DESC")
    List<VideoPurchase> findPurchasesOfOwnerVideos(@Param("ownerId") String ownerId);
}
