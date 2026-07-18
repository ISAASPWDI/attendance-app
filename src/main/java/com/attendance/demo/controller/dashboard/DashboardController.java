package com.attendance.demo.controller.dashboard;

import com.attendance.demo.dto.attendances.DashboardSummaryDTO;
import com.attendance.demo.service.attendances.AttendanceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
