package com.attendance.demo.dto.filter;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class AttendanceFilter {
    private String teacherName;
    private String status;
    private LocalDate fromDate;
    private LocalDate toDate;
    /** MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY | SUNDAY */
    private String dayOfWeek;
    /** date | teacherName | status */
    private String sortBy;
    /** asc | desc (default: desc) */
    private String order;
}
