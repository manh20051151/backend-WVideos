package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
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

    // Native query - bỏ qua @SQLRestriction
    @Query(value = "SELECT * FROM users WHERE id = :userId", nativeQuery = true)
    Optional<User> findByIdIncludingLocked(@Param("userId") String userId);
    
    // Native query - bỏ qua @SQLRestriction và lấy user kèm roles
    @Query(value = """
        SELECT u.id, u.password, u.number_phone, u.full_name, u.avatar, u.email, 
               u.gender, u.bank_name, u.bank_account_holder_name, u.bank_account_number,
               r.id as role_id, r.name as role_name, r.description as role_description
        FROM users u 
        LEFT JOIN user_roles ur ON u.id = ur.user_id 
        LEFT JOIN roles r ON ur.role_id = r.id 
        WHERE u.email = :email
        """, nativeQuery = true)
    List<Object[]> findUserWithRolesByEmail(@Param("email") String email);
    
    // Native query - bỏ qua @SQLRestriction và lấy user kèm roles theo ID
    @Query(value = """
        SELECT u.id, u.password, u.number_phone, u.full_name, u.avatar, u.email, 
               u.gender, u.bank_name, u.bank_account_holder_name, u.bank_account_number,
               r.id as role_id, r.name as role_name, r.description as role_description
        FROM users u 
        LEFT JOIN user_roles ur ON u.id = ur.user_id 
        LEFT JOIN roles r ON ur.role_id = r.id 
        WHERE u.id = :userId
        """, nativeQuery = true)
    List<Object[]> findUserWithRolesById(@Param("userId") String userId);
    
    // Native update - bỏ qua @SQLRestriction
    @Modifying
    @Query(value = """
        UPDATE users 
        SET full_name = COALESCE(:fullName, full_name),
            number_phone = COALESCE(:numberPhone, number_phone),
            gender = COALESCE(:gender, gender),
            avatar = COALESCE(:avatar, avatar),
            bank_name = COALESCE(:bankName, bank_name),
            bank_account_holder_name = COALESCE(:bankAccountHolderName, bank_account_holder_name),
            bank_account_number = COALESCE(:bankAccountNumber, bank_account_number)
        WHERE email = :email
        """, nativeQuery = true)
    void updateUserInfo(@Param("email") String email, @Param("fullName") String fullName, @Param("numberPhone") String numberPhone, @Param("gender") Boolean gender, @Param("avatar") String avatar, @Param("bankName") String bankName, @Param("bankAccountHolderName") String bankAccountHolderName, @Param("bankAccountNumber") String bankAccountNumber);
}
