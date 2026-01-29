package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Bỏ qua @SQLRestriction(locked = false) khi cần kiểm tra login, để phân biệt rõ user bị khóa
    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingLocked(@Param("email") String email);

    // Tìm các User có cả position và organizerRole khác null

    List<User> findByRoles_Name(String roleName);

    // Tìm user đã bị khóa bằng native query
    @Query(value = "SELECT * FROM users WHERE id = ?1 AND locked = true", nativeQuery = true)
    Optional<User> findLockedUserById(String userId);

    // Lấy danh sách user bị khóa
    @Query(value = "SELECT * FROM users WHERE locked = true", nativeQuery = true)
    Page<User> findLockedUsers(Pageable pageable);

    // Đếm tổng số user theo trạng thái khóa
    long countByLocked(boolean locked);

    // Đếm số user đăng ký trong khoảng thời gian (dùng cho thống kê admin)
    @Query("SELECT COUNT(u) FROM User u WHERE u.joinedDate BETWEEN :from AND :to")
    long countUsersJoinedBetween(@Param("from") java.util.Date from, @Param("to") java.util.Date to);

    // Thống kê số user đăng ký theo tháng (toàn hệ thống)
    @Query("SELECT YEAR(u.joinedDate), MONTH(u.joinedDate), COUNT(u) FROM User u " +
           "GROUP BY YEAR(u.joinedDate), MONTH(u.joinedDate) " +
           "ORDER BY YEAR(u.joinedDate), MONTH(u.joinedDate)")
    java.util.List<Object[]> countMonthlyUserRegistrations();
    
    // Thống kê tài liệu của user - DISABLED (Document entity not available)
    // @Query("SELECT COUNT(d) FROM Document d WHERE d.user.id = :userId")
    // Long countTotalDocumentsByUserId(@Param("userId") String userId);
    
    // @Query("SELECT COUNT(d) FROM Document d WHERE d.user.id = :userId AND d.status = 'APPROVED'")
    // Long countApprovedDocumentsByUserId(@Param("userId") String userId);
    
    // @Query("SELECT COALESCE(SUM(d.viewCount), 0) FROM Document d WHERE d.user.id = :userId")
    // Long getTotalViewsByUserId(@Param("userId") String userId);
    
    // @Query("SELECT COALESCE(SUM(d.downloadCount), 0) FROM Document d WHERE d.user.id = :userId")
    // Long getTotalDownloadsByUserId(@Param("userId") String userId);
    
    // Load user kèm theo danh sách tài liệu đã mua - DISABLED (Document entity not available)
    // @Query("SELECT u FROM User u LEFT JOIN FETCH u.purchasedDocuments WHERE u.id = :userId")
    // Optional<User> findByIdWithPurchasedDocuments(@Param("userId") String userId);
    
    // Load user kèm theo roles (without purchasedDocuments)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :userId")
    Optional<User> findByIdWithRolesAndPurchasedDocuments(@Param("userId") String userId);
}
