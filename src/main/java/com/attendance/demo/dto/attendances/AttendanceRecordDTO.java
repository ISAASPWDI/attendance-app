package com.attendance.demo.dto.attendances;

import com.attendance.demo.entity.AttendanceRecord;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordDTO {

    @NotNull
    private Long attendanceId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime timeIn;

    @NotNull
    private LocalTime timeOut;

    @NotNull
    private String status;

    @Nullable
    private String notes;
}