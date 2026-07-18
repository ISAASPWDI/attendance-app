package com.attendance.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@ToString(exclude = "password")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(unique = true, length = 150)
    private String email;

    // Not DB-NOT-NULL: ddl-auto=update can't backfill existing rows atomically.
    // Application code always sets this explicitly; existing rows are backfilled to true via SQL.
    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "signature_url", length = 500)
    private String signatureUrl;

    @Column(name = "fingerprint_url", length = 500)
    private String fingerprintUrl;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    // Legacy field – kept nullable so existing schema stays compatible.
    // Column sign_field was previously NOT NULL; Hibernate will relax it on ddl-auto=update.
    @Column(name = "sign_field", length = 50)
    private String signField;

    public enum Role {
        TEACHER, DIRECTOR
    }

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<AttendanceRecord> attendanceRecords;

    public String getFullName() {
        if (firstName != null && lastName != null) return firstName + " " + lastName;
        if (firstName != null) return firstName;
        if (lastName != null) return lastName;
        return username;
    }
}
