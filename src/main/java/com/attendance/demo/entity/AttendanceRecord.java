package com.attendance.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;


@Entity()
@Table(name = "attendance_record")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate date;

    @Column(nullable = false)
    private LocalTime timeIn;

    private LocalTime timeOut;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String notes;
    public enum Status{
        Present, Late, Absent
    }
}
