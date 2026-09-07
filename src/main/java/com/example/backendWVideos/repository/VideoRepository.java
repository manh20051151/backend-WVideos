package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.enums.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    
    Optional<Video> findByFileCode(String fileCode);
    
    // Tối ưu: Fetch categories và tags cùng lúc cho findById
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    Optional<Video> findById(String id);
    
    // Tối ưu: Fetch categories và tags cùng lúc để tránh N+1
    @EntityGraph(attributePaths = {"categories", "tags"})
    Page<Video> findByUserId(String userId, Pageable pageable);
    
    // Tối ưu: Fetch categories, user và tags cùng lúc cho public videos
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    Page<Video> findByStatusAndIsPublic(VideoStatus status, Boolean isPublic, Pageable pageable);
    
    // JPQL query với EntityGraph để tránh lazy loading issue
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' AND v.isPublic = true ORDER BY v.createdAt DESC")
    Page<Video> findPublicVideosNative(Pageable pageable);
    
    // JPQL query với custom sort
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' AND v.isPublic = true ORDER BY v.views DESC")
    Page<Video> findPublicVideosByViews(Pageable pageable);
    
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' AND v.isPublic = true ORDER BY v.favoritesCount DESC")
    Page<Video> findPublicVideosByFavorites(Pageable pageable);
    
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' AND v.isPublic = true ORDER BY v.commentsCount DESC")
    Page<Video> findPublicVideosByComments(Pageable pageable);
    
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' AND v.isPublic = true ORDER BY v.duration DESC")
    Page<Video> findPublicVideosByDuration(Pageable pageable);
    
    // All videos queries (bao gồm cả không công khai) - dùng DISTINCT để tránh warning
    @Query("SELECT DISTINCT v FROM Video v LEFT JOIN FETCH v.categories LEFT JOIN FETCH v.user LEFT JOIN FETCH v.tags WHERE v.status = 'READY' ORDER BY v.createdAt DESC")
    Page<Video> findAllVideosByCreatedAt(Pageable pageable);
    
    @Query("SELECT DISTINCT v FROM Video v LEFT JOIN FETCH v.categories LEFT JOIN FETCH v.user LEFT JOIN FETCH v.tags WHERE v.status = 'READY' ORDER BY v.views DESC")
    Page<Video> findAllVideosByViews(Pageable pageable);
    
    @Query("SELECT DISTINCT v FROM Video v LEFT JOIN FETCH v.categories LEFT JOIN FETCH v.user LEFT JOIN FETCH v.tags WHERE v.status = 'READY' ORDER BY v.favoritesCount DESC")
    Page<Video> findAllVideosByFavorites(Pageable pageable);
    
    @Query("SELECT DISTINCT v FROM Video v LEFT JOIN FETCH v.categories LEFT JOIN FETCH v.user LEFT JOIN FETCH v.tags WHERE v.status = 'READY' ORDER BY v.commentsCount DESC")
    Page<Video> findAllVideosByComments(Pageable pageable);
    
    @Query("SELECT DISTINCT v FROM Video v LEFT JOIN FETCH v.categories LEFT JOIN FETCH v.user LEFT JOIN FETCH v.tags WHERE v.status = 'READY' ORDER BY v.duration DESC")
    Page<Video> findAllVideosByDuration(Pageable pageable);
    
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    Page<Video> findByUserIdAndStatusNot(String userId, VideoStatus status, Pageable pageable);
    
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    Page<Video> findByUserIdAndStatusAndIsPublicTrue(String userId, VideoStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    Page<Video> findByUserIdAndStatus(String userId, VideoStatus status, Pageable pageable);

    // Lấy các video đang xử lý của Streamtape để scheduler re-check trạng thái convert
    @EntityGraph(attributePaths = {"user"})
    Page<Video> findByStatusAndProvider(VideoStatus status, String provider, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Video> findByStatus(VideoStatus status, Pageable pageable);

    Long countByUserId(String userId);
    
    Long countByUserIdAndStatus(String userId, VideoStatus status);
    
    // Đếm video không bao gồm status nhất định
    Long countByUserIdAndStatusNot(String userId, VideoStatus status);
    
    // Thêm method đếm video theo status cho dashboard
    Long countByStatus(VideoStatus status);
    
    /**
     * Tăng lượt xem video một cách atomic để tối ưu performance
     */
    @Modifying
    @Query("UPDATE Video v SET v.views = v.views + 1 WHERE v.id = :videoId")
    int incrementViewsById(@Param("videoId") String videoId);
    
    // Lấy tổng lượt xem của user
    @Query("SELECT COALESCE(SUM(v.views), 0) FROM Video v WHERE v.user.id = :userId AND v.status != :status")
    Long getTotalViewsByUserId(@Param("userId") String userId, @Param("status") VideoStatus status);

    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.id != :currentVideoId AND v.status = 'READY' AND v.isPublic = true " +
           "AND (EXISTS (SELECT c FROM v.categories c WHERE c.id IN :categoryIds) OR " +
           "EXISTS (SELECT t FROM v.tags t WHERE t IN :tags) OR v.user.id = :videoUserId) " +
           "ORDER BY " +
           "CASE WHEN v.user.id = :videoUserId THEN 1 ELSE 0 END DESC, " +
           "CASE WHEN EXISTS (SELECT c FROM v.categories c WHERE c.id IN :categoryIds) THEN 1 ELSE 0 END DESC, " +
           "CASE WHEN EXISTS (SELECT t FROM v.tags t WHERE t IN :tags) THEN 1 ELSE 0 END DESC, " +
           "v.views DESC, v.createdAt DESC")
    Page<Video> findRelatedVideos(@Param("currentVideoId") String currentVideoId,
                                 @Param("videoUserId") String videoUserId,
                                 @Param("categoryIds") List<String> categoryIds,
                                 @Param("tags") List<String> tags,
                                 Pageable pageable);
}
