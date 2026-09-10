package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.VideoPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoPurchaseRepository extends JpaRepository<VideoPurchase, String> {

    List<VideoPurchase> findByUserIdAndVideoId(String userId, String videoId);

    boolean existsByUserIdAndVideoId(String userId, String videoId);
}
