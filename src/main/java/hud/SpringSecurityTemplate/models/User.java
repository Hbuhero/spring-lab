package hud.SpringSecurityTemplate.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "USERS",indexes = @Index(columnList = "EMAIL,PHONE_NUMBER",name = "INDEX_USERS"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "EMAIL", unique = true)
    private String email;

    @Column(name = "PHONE_NUMBER", unique = true)
    private String phoneNumber;

    @Column(name = "ROLE")
    private String role;

    @Column(name = "PERMISSIONS" ,columnDefinition="TEXT")
    private String permissions;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "IMAGE")
    private String image;

    @Column(name = "PROVIDER")
    private String provider;

    @Column(name = "PASSWORD")
    private String password;

    @Builder.Default
    @Column(name = "PASSWORD_CHANGED")
    private Boolean passwordChanged=false;

    @JsonIgnore
    @Column(name = "PASSWORD_RESET_TOKEN")
    private String passwordResetToken;

    @JsonIgnore
    @Column(name = "PASSWORD_RESET_TOKEN_EXPIRATION")
    private LocalDateTime passwordResetTokenExpiration;

    @Column(name = "REFRESH_TOKEN")
    private String refreshToken;

    @Builder.Default
    @JsonIgnore
    @Column(name = "REFRESH_TOKEN_EXPIRATION")
    private LocalDateTime refreshTokenExpiration = LocalDateTime.now();

    @Column(name = "OTP")
    @JsonIgnore
    private String otp;

    @Builder.Default
    @Column(name = "IS_OTP_VERIFIED")
    private Boolean isOtpVerified = false;

    @Builder.Default
    @Column(name = "OTP_EXPIRATION")
    @JsonIgnore
    private LocalDateTime otpExpiration = LocalDateTime.now();

    @Column(name = "OAUTH_EXCHANGE_CODE")
    private String oauthExchangeCode;

    @Column(name = "OAUTH_EXCHANGE_CODE_EXPIRATION")
    private LocalDateTime oauthExchangeCodeExpiration;

    @CreationTimestamp
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}