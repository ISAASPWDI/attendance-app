package com.attendance.demo.controller.dashboard;

import com.attendance.demo.dto.attendances.DashboardSummaryDTO;
import com.attendance.demo.dto.attendances.PurgeWarningDTO;
import com.attendance.demo.service.attendances.AttendanceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAuthority('DIRECTOR')")
public class DashboardController {

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(attendanceRecordService.getDashboardSummary());
    }

    /** Warns during the last 7 days of the month that attendance history will be purged. Any authenticated user (TEACHER or DIRECTOR). */
    @GetMapping("/purge-warning")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurgeWarningDTO> getPurgeWarning() {
        LocalDate today = LocalDate.now();
        LocalDate lastDay = today.with(TemporalAdjusters.lastDayOfMonth());
        long daysRemaining = ChronoUnit.DAYS.between(today, lastDay);
        boolean active = daysRemaining <= 6;
        return ResponseEntity.ok(new PurgeWarningDTO(active, active ? daysRemaining : null, active ? lastDay : null));
    }
}
