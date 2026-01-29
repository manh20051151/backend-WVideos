package com.example.backendWVideos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.example.backendWVideos.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
//@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SQLRestriction("locked = false") // Tự động thêm điều kiện này vào các câu query
public class User {
    

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    String username;
    String password;
    String numberPhone;
    String fullName;
    LocalDate dob;
    // @Column(name = "image_url")
    String avatar; // URL ảnh từ OAuth2 hoặc Cloudinary
    @Column(unique = true)
    String email;
    @Column(name = "email_verified")
    Boolean emailVerified = false;
    Boolean gender; // True là nam

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    AuthProvider authProvider = AuthProvider.GOOGLE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    Set<Role> roles;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    boolean locked = false; // Trạng thái khóa tài khoản

    @Column(name = "locked_at")
    @Temporal(TemporalType.TIMESTAMP)
    Date lockedAt; // Thời điểm khóa

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    @JsonIgnore
    User lockedBy; // Người thực hiện khóa

    @Column(name = "lock_reason")
    String lockReason; // Lý do khóa

    @Column(name = "joined_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date joinedDate; // Ngày tham gia hệ thống

    // Thông tin tài chính
    @Column(name = "balance", columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    @Builder.Default
    private Double balance = 0.0; // Số dư tài khoản

    @Column(name = "revenue", columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    @Builder.Default
    private Double revenue = 0.0; // Doanh thu từ bán tài liệu
    
    // Thông tin ngân hàng
    @Column(name = "bank_name", length = 100)
    private String bankName; // Tên ngân hàng (VD: Vietcombank, Techcombank, BIDV...)
    
    @Column(name = "bank_account_holder_name", length = 100)
    private String bankAccountHolderName; // Tên chủ tài khoản ngân hàng
    
    @Column(name = "bank_account_number", length = 20)
    private String bankAccountNumber; // Số tài khoản ngân hàng
    
    // Phương thức khóa tài khoản
    public void lock(User lockedByUser, String reason) {
        this.locked = true;
        this.lockedAt = new Date();
        this.lockedBy = lockedByUser;
        this.lockReason = reason;
    }

    // Phương thức mở khóa tài khoản
    public void unlock() {
        this.locked = false;
        this.lockedAt = null;
        this.lockedBy = null;
        this.lockReason = null;
    }

    @PrePersist
    protected void onCreate() {
        joinedDate = new Date(); // Tự động set ngày hiện tại khi tạo mới
        
        // Khởi tạo thông tin tài chính nếu chưa có
        if (balance == null) {
            balance = 0.0;
        }
        if (revenue == null) {
            revenue = 0.0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
