package com.attendance.demo.dto.attendances;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceRecordResponseDTO {

    // TODO: change this.
//    @NotNull
//    private LocalTime startTime;
//    @NotNull
//    private LocalTime endTime;
    @NotNull
    private Long id;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime timeIn;

    @Null
    private LocalTime timeOut;

    @NotNull
    private String status;

    @Null
    private String notes;
}
