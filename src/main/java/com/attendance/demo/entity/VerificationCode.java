package com.attendance.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_codes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "purpose"}))
@Getter @Setter
@NoArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 4)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Purpose {
        EMAIL_VERIFICATION, PASSWORD_RESET
    }

    public static VerificationCode create(Long userId, String code, Purpose purpose, long validMinutes) {
        VerificationCode vc = new VerificationCode();
        vc.userId = userId;
        vc.code = code;
        vc.purpose = purpose;
        vc.createdAt = LocalDateTime.now();
        vc.expiresAt = vc.createdAt.plusMinutes(validMinutes);
        return vc;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean matches(String candidate) {
        return code.equals(candidate);
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
