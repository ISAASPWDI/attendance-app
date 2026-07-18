package com.attendance.demo.entity;

import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class AttendanceRecordByUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private LocalDateTime createTime;

    @ManyToOne
    @JoinColumn(name = "attendance_record_id")
    private AttendanceRecord attendanceRecord;

    @ManyToOne()
    @JoinColumn(name = "user_id")
    private User user;

}
