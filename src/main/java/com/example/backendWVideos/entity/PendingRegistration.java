package com.example.backendWVideos.entity;

import com.example.backendWVideos.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PendingRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    String username;
    String password;
    String numberPhone;
    String fullName;
    String email;
    String token;
    AuthProvider authProvider;
    @Column(nullable = false)
    LocalDateTime expiryDate;
    
    @Column(nullable = false)
    boolean confirmed = false;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
