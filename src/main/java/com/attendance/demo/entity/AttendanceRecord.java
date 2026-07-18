package com.attendance.demo.entity;

import com.attendance.demo.exception.attendances.RecordTimeInException;
import com.attendance.demo.exception.attendances.RecordTimeOutException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance_record")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime timeIn;

    private LocalTime timeOut;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    private String notes;

    public enum Status {
        Present, Late, Absent
    }

    public void setTimeIn(LocalTime timeIn) {
        if (this.timeOut != null && timeIn != null && timeIn.isAfter(this.timeOut)) {
            throw new RecordTimeInException(timeIn);
        }
        this.timeIn = timeIn;
    }

    public void setTimeOut(LocalTime timeOut) {
        if (timeOut != null && this.timeIn != null && timeOut.isBefore(this.timeIn)) {
            throw new RecordTimeOutException(timeOut);
        }
        this.timeOut = timeOut;
    }
}
