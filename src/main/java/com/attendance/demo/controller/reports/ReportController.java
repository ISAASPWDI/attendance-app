package com.attendance.demo.controller.reports;

import com.attendance.demo.dto.filter.AttendanceFilter;
import com.attendance.demo.service.attendances.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAuthority('DIRECTOR')")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /** Download Excel report with the same filters as the attendance list. */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(AttendanceFilter filter) throws Exception {
        byte[] data = reportService.generateExcel(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"asistencias.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** Download PDF report with the same filters as the attendance list. */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(AttendanceFilter filter) throws Exception {
        byte[] data = reportService.generatePdf(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"asistencias.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
