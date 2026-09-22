package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.PasswordResetToken;
import com.example.backendWVideos.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUser(User user);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId")
    List<PasswordResetToken> findAllByUserId(@Param("userId") String userId);

    void deleteAllByUser(User user);
}
