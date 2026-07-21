package com.attendance.demo.controller.attendances;

import com.attendance.demo.config.CustomUserDetails;
import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import com.attendance.demo.dto.attendances.AttendanceRecordResponseDTO;
import com.attendance.demo.dto.attendances.AttendanceRecordWithUserDTO;
import com.attendance.demo.dto.filter.AttendanceFilter;
import com.attendance.demo.service.attendances.AttendanceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceRecordController {

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    // ── TEACHER ──────────────────────────────────────────────────────────────

    /** Today's attendance for the authenticated teacher (null body = 204 if not found). */
    @GetMapping("/today")
    public ResponseEntity<AttendanceRecordResponseDTO> getTodayRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AttendanceRecordResponseDTO record = attendanceRecordService.getTodayRecord(userDetails.user.getId());
        return record != null ? ResponseEntity.ok(record) : ResponseEntity.noContent().build();
    }

    /** Manual attendance creation (custom time/status). */
    @PostMapping
    public ResponseEntity<AttendanceRecordResponseDTO> create(
            @RequestBody AttendanceRecordDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceRecordService.create(dto, userDetails.user.getId()));
    }

    /** Quick check-in at current server time; status auto-set vs 7:30 cutoff. */
    @PostMapping("/quick-checkin")
    public ResponseEntity<AttendanceRecordResponseDTO> quickCheckIn(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceRecordService.quickCheckIn(userDetails.user.getId()));
    }

    /** Quick check-out at current server time; only allowed after 13:00. */
    @PostMapping("/quick-checkout")
    public ResponseEntity<AttendanceRecordResponseDTO> quickCheckOut(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceRecordService.quickCheckOut(userDetails.user.getId()));
    }

    /** Partial update of an existing attendance record. */
    @PatchMapping("/{id}")
    public ResponseEntity<AttendanceRecordResponseDTO> patch(
            @PathVariable Long id,
            @RequestBody AttendanceRecordDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceRecordService.patch(id, dto, userDetails.user.getId()));
    }

    /** Paginated attendance history for the authenticated user's own records (teacher or director). */
    @GetMapping("/me")
    public ResponseEntity<Page<AttendanceRecordResponseDTO>> getMyAttendances(
            AttendanceFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceRecordService.getMyAttendancePage(userDetails.user.getId(), filter, pageable));
    }

    // ── DIRECTOR ─────────────────────────────────────────────────────────────

    /** Paginated attendance list with filters. DIRECTOR only. */
    @GetMapping
    @PreAuthorize("hasAuthority('DIRECTOR')")
    public ResponseEntity<Page<AttendanceRecordWithUserDTO>> getAttendances(
            AttendanceFilter filter,
            Pageable pageable) {
        return ResponseEntity.ok(attendanceRecordService.getAttendancePage(filter, pageable));
    }

    /**
     * Delete all records for a specific date (use before generating the daily report
     * to keep the free DB plan under the 512 MB limit). DIRECTOR only.
     */
    @DeleteMapping("/by-date/{date}")
    @PreAuthorize("hasAuthority('DIRECTOR')")
    public ResponseEntity<Map<String, Object>> deleteByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        long deleted = attendanceRecordService.deleteByDate(date);
        return ResponseEntity.ok(Map.of("deletedCount", deleted, "date", date.toString()));
    }
}
