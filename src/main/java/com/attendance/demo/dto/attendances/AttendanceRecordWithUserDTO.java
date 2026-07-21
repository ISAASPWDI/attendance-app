package com.attendance.demo.dto.attendances;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceRecordWithUserDTO {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private LocalDate date;
    private String dayOfWeek;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private String status;
    private String notes;
}
