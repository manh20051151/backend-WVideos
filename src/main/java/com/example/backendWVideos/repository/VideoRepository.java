package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.Video;
import com.example.backendWVideos.entity.WatchedVideo;
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

import java.time.LocalDateTime;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {

    // Tìm theo slug (URL-friendly) - fetch categories, user, tags để tránh LazyInitializationException khi serialize
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    Optional<Video> findBySlug(String slug);

    // Lấy các video chưa có slug (để backfill)
    List<Video> findAllBySlugIsNull();

    // Kiểm tra slug đã tồn tại
    boolean existsBySlug(String slug);
    
    Optional<Video> findByFileCode(String fileCode);
    
    // Tối ưu: Fetch categories và tags cùng lúc cho findById
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    Optional<Video> findById(String id);

    // Tối ưu: Fetch categories, user, tags cho danh sách id (tránh LazyInitialization khi serialize)
    @Override
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    List<Video> findAllById(Iterable<String> ids);
    
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

    // Video công khai theo slug của category (dropdown Thể loại + trang /category/{slug})
    // Sort truyền qua Pageable (createdAt/views/favoritesCount/commentsCount/duration)
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT DISTINCT v FROM Video v JOIN v.categories c WHERE v.status = 'READY' AND v.isPublic = true AND c.slug = :categorySlug")
    Page<Video> findPublicVideosByCategorySlug(@Param("categorySlug") String categorySlug, Pageable pageable);

    // Video công khai có chứa tag (trang /tag/{tag} khi click tag ở watch page)
    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' AND v.isPublic = true AND :tag MEMBER OF v.tags")
    Page<Video> findPublicVideosByTag(@Param("tag") String tag, Pageable pageable);
    
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
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Video v SET v.views = v.views + 1 WHERE v.id = :videoId")
    int incrementViewsById(@Param("videoId") String videoId);

    // Đồng bộ cột favorites_count = số LIKE thật trong bảng video_reactions
    // (chạy 1 lần lúc khởi động để sửa dữ liệu cũ, toggleReaction tự giữ đồng bộ về sau)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE videos v SET v.favorites_count = " +
            "(SELECT COUNT(*) FROM video_reactions r WHERE r.video_id = v.id AND r.reaction_type = 'LIKE')",
            nativeQuery = true)
    int backfillFavoritesCount();
    
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

    // === Shorts feed ===
    // Hiện TẤT CẢ video dưới 2 phút (120 giây), kể cả video riêng tư và video có phí.
    // Video có phí -> frontend hiện "phải mua", video riêng tư -> phải đăng nhập mới xem được.

    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' " +
           "AND v.duration IS NOT NULL AND v.duration < 120 " +
           "AND (:lastCreatedAt IS NULL OR v.createdAt < :lastCreatedAt) " +
           "ORDER BY v.createdAt DESC")
    List<Video> findShorts(@Param("lastCreatedAt") LocalDateTime lastCreatedAt, Pageable pageable);

    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' " +
           "AND v.duration IS NOT NULL AND v.duration < 120 " +
           "AND (:lastCreatedAt IS NULL OR v.createdAt < :lastCreatedAt) " +
           "AND NOT EXISTS (SELECT w FROM WatchedVideo w WHERE w.userId = :userId AND w.videoId = v.id) " +
           "ORDER BY v.createdAt DESC")
    List<Video> findShortsExcludingWatched(@Param("userId") String userId,
                                            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                                            Pageable pageable);

    // === Tìm kiếm thông minh (header search) ===
    // Tìm video theo tiêu đề, mô tả hoặc tag. Ưu tiên video nhiều view trước.

    @EntityGraph(attributePaths = {"categories", "user", "tags"})
    @Query("SELECT v FROM Video v WHERE v.status = 'READY' AND " +
           "(LOWER(v.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(v.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "EXISTS (SELECT 1 FROM Video v2 JOIN v2.tags t WHERE v2.id = v.id AND LOWER(t) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY v.views DESC, v.createdAt DESC")
    Page<Video> searchByKeyword(@Param("q") String q, Pageable pageable);

    // === Thống kê kênh của người dùng ===

    // Đếm số video theo trạng thái (không tính DELETED)
    @Query("SELECT v.status, COUNT(v) FROM Video v WHERE v.user.id = :userId AND v.status <> com.example.backendWVideos.enums.VideoStatus.DELETED GROUP BY v.status")
    List<Object[]> countOwnerVideosByStatus(@Param("userId") String userId);

    // Tổng bình luận + tổng lượt lưu cộng dồn (không tính video đã xóa)
    @Query("SELECT COALESCE(SUM(v.commentsCount), 0), COALESCE(SUM(v.favoritesCount), 0) " +
           "FROM Video v WHERE v.user.id = :userId AND v.status <> com.example.backendWVideos.enums.VideoStatus.DELETED")
    List<Object[]> sumOwnerEngagement(@Param("userId") String userId);
    // Thời điểm video sớm nhất của kênh - dùng cho xu hướng "toàn bộ thời gian"
    @Query("SELECT MIN(v.createdAt) FROM Video v WHERE v.user.id = :userId")
    LocalDateTime oldestOwnerVideoCreatedAt(@Param("userId") String userId);
}
