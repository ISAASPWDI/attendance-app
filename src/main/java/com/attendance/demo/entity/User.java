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
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        TEACHER, DIRECTOR
    }

    @OneToMany(mappedBy = "user")
    List<AttendanceRecord> attendanceRecords;
//    public User(Long id, String username, String password, Role role) {
//        this.id = id;
//        this.username = username;
//        this.password = password;
//        this.role = role;
//    }

}
