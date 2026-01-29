package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {
    Optional<PendingRegistration> findByToken(String token);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
