package com.attendance.demo.service.attendances;

import com.attendance.demo.dto.attendances.AttendanceRecordResponseDTO;
import com.attendance.demo.dto.attendances.AttendanceRecordWithUserDTO;
import com.attendance.demo.dto.attendances.DashboardSummaryDTO;
import com.attendance.demo.dto.attendances.DayStatusDTO;
import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import com.attendance.demo.dto.filter.AttendanceFilter;
import com.attendance.demo.entity.AttendanceRecord;
import com.attendance.demo.entity.User;
import com.attendance.demo.exception.attendances.RecordAlreadyExistsException;
import com.attendance.demo.exception.attendances.RecordHolidayException;
import com.attendance.demo.exception.attendances.RecordNotFoundException;
import com.attendance.demo.exception.attendances.RecordTimeInWindowException;
import com.attendance.demo.exception.attendances.RecordTimeOutWindowException;
import com.attendance.demo.exception.users.UserNotFoundException;
import com.attendance.demo.repository.AttendanceRepository;
import com.attendance.demo.repository.UserRepository;
import com.attendance.demo.specification.AttendanceSpecification;
import com.attendance.demo.util.DayOfWeekEs;
import com.attendance.demo.util.PeruHolidays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class AttendanceRecordService {

    /** Entry window: 7:30-8:20 = Present, 8:20-9:00 = Late, outside 7:30-9:00 = closed. */
    private static final LocalTime ENTRY_WINDOW_START = LocalTime.of(7, 30);
    private static final LocalTime ENTRY_LATE_CUTOFF  = LocalTime.of(8, 20);
    private static final LocalTime ENTRY_WINDOW_END   = LocalTime.of(9, 0);
    /** Exit window: 1:00-2:00 pm, open the whole time (1:32 pm is only an informational on-time/late split, not a close). */
    private static final LocalTime EXIT_WINDOW_START  = LocalTime.of(13, 0);
    private static final LocalTime EXIT_WINDOW_END    = LocalTime.of(14, 0);

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    // ── TEACHER endpoints ─────────────────────────────────────────────────────

    /** Manual check-in (custom time + status). */
    @Transactional
    public AttendanceRecordResponseDTO create(AttendanceRecordDTO dto, Long userId) {
        if (attendanceRepository.findByUserIdAndDate(userId, dto.getDate()).isPresent()) {
            throw new RecordAlreadyExistsException(dto.getDate());
        }
        validateNotHoliday(dto.getDate());
        validateTimeIn(dto.getTimeIn());
        validateTimeOut(dto.getTimeOut());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        AttendanceRecord r = new AttendanceRecord();
        r.setUser(user);
        r.setDate(dto.getDate());
        r.setTimeIn(dto.getTimeIn());
        r.setTimeOut(dto.getTimeOut());
        r.setStatus(AttendanceRecord.Status.valueOf(dto.getStatus()));
        r.setNotes(dto.getNotes());

        return toDTO(attendanceRepository.save(r));
    }

    /** Quick check-in at current time; only allowed between 7:30 and 9:00 am. Status auto-calculated vs 8:20 cutoff. */
    @Transactional
    public AttendanceRecordResponseDTO quickCheckIn(Long userId) {
        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByUserIdAndDate(userId, today).isPresent()) {
            throw new RecordAlreadyExistsException(today);
        }
        validateNotHoliday(today);
        LocalTime now = LocalTime.now();
        if (now.isBefore(ENTRY_WINDOW_START) || now.isAfter(ENTRY_WINDOW_END)) {
            throw new IllegalStateException("La entrada rápida solo está habilitada de 7:30 am a 9:00 am");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        AttendanceRecord.Status status = now.isAfter(ENTRY_LATE_CUTOFF)
                ? AttendanceRecord.Status.Late
                : AttendanceRecord.Status.Present;

        AttendanceRecord r = new AttendanceRecord();
        r.setUser(user);
        r.setDate(today);
        r.setTimeIn(now);
        r.setStatus(status);

        return toDTO(attendanceRepository.save(r));
    }

    /** Quick check-out at current time. Only allowed between 1:00 pm and 2:00 pm. */
    @Transactional
    public AttendanceRecordResponseDTO quickCheckOut(Long userId) {
        validateNotHoliday(LocalDate.now());
        LocalTime now = LocalTime.now();
        if (now.isBefore(EXIT_WINDOW_START) || now.isAfter(EXIT_WINDOW_END)) {
            throw new IllegalStateException("La salida rápida solo está habilitada de 1:00 pm a 2:00 pm");
        }
        AttendanceRecord r = attendanceRepository
                .findByUserIdAndDate(userId, LocalDate.now())
                .orElseThrow(() -> new RecordNotFoundException(
                        "No se encontró registro de asistencia de hoy para este docente"));

        r.setTimeOut(now);
        return toDTO(attendanceRepository.save(r));
    }

    /** Whether today is a Peru holiday or a weekend — check-in/check-out are blocked on both. */
    @Transactional(readOnly = true)
    public DayStatusDTO getTodayStatus() {
        LocalDate today = LocalDate.now();
        DayOfWeek dow = today.getDayOfWeek();
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        boolean holiday = PeruHolidays.isHoliday(today);
        return new DayStatusDTO(holiday, holiday ? PeruHolidays.nameOf(today) : null, weekend);
    }

    /** Returns today's attendance record for the authenticated teacher, if any. */
    @Transactional(readOnly = true)
    public AttendanceRecordResponseDTO getTodayRecord(Long userId) {
        return attendanceRepository.findByUserIdAndDate(userId, LocalDate.now())
                .map(this::toDTO)
                .orElse(null);
    }

    /** Partial update (PATCH) of an existing record. */
    @Transactional
    public AttendanceRecordResponseDTO patch(Long recordId, AttendanceRecordDTO dto, Long userId) {
        AttendanceRecord r = attendanceRepository.findById(recordId)
                .orElseThrow(() -> new RecordNotFoundException(recordId));

        if (dto.getDate() != null)   r.setDate(dto.getDate());
        if (dto.getTimeIn() != null) {
            validateTimeIn(dto.getTimeIn());
            r.setTimeIn(dto.getTimeIn());
        }
        if (dto.getTimeOut() != null) {
            validateTimeOut(dto.getTimeOut());
            r.setTimeOut(dto.getTimeOut());
        }
        if (dto.getStatus() != null) r.setStatus(AttendanceRecord.Status.valueOf(dto.getStatus()));
        if (dto.getNotes() != null)  r.setNotes(dto.getNotes());

        return toDTO(attendanceRepository.save(r));
    }

    private void validateTimeIn(LocalTime timeIn) {
        if (timeIn != null && (timeIn.isBefore(ENTRY_WINDOW_START) || timeIn.isAfter(ENTRY_WINDOW_END))) {
            throw new RecordTimeInWindowException(timeIn);
        }
    }

    private void validateTimeOut(LocalTime timeOut) {
        if (timeOut != null && (timeOut.isBefore(EXIT_WINDOW_START) || timeOut.isAfter(EXIT_WINDOW_END))) {
            throw new RecordTimeOutWindowException(timeOut);
        }
    }

    private void validateNotHoliday(LocalDate date) {
        if (PeruHolidays.isHoliday(date)) {
            throw new RecordHolidayException(date, PeruHolidays.nameOf(date));
        }
    }

    /** Paginated attendance history for the authenticated user's own records (teacher or director). */
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponseDTO> getMyAttendancePage(Long userId, AttendanceFilter filter, Pageable pageable) {
        Specification<AttendanceRecord> spec = AttendanceSpecification.filter(filter)
                .and(AttendanceSpecification.forUser(userId));
        return attendanceRepository.findAll(spec, pageable).map(this::toDTO);
    }

    // ── DIRECTOR endpoints ────────────────────────────────────────────────────

    /** Director backfill for another user's missed check-in/out on a past working day; skips only the time-window checks. */
    @Transactional
    public AttendanceRecordResponseDTO createForUser(Long targetUserId, AttendanceRecordDTO dto) {
        if (attendanceRepository.findByUserIdAndDate(targetUserId, dto.getDate()).isPresent()) {
            throw new RecordAlreadyExistsException(dto.getDate());
        }
        if (dto.getDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("No se puede registrar asistencia en una fecha futura");
        }
        validateNotHoliday(dto.getDate());

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        AttendanceRecord r = new AttendanceRecord();
        r.setUser(user);
        r.setDate(dto.getDate());
        r.setTimeIn(dto.getTimeIn());
        r.setTimeOut(dto.getTimeOut());
        r.setStatus(AttendanceRecord.Status.valueOf(dto.getStatus()));
        r.setNotes(dto.getNotes());

        return toDTO(attendanceRepository.save(r));
    }

    /** Paginated attendance records with optional filters (director view). */
    @Transactional(readOnly = true)
    public Page<AttendanceRecordWithUserDTO> getAttendancePage(AttendanceFilter filter, Pageable pageable) {
        Specification<AttendanceRecord> spec = AttendanceSpecification.filter(filter);
        return attendanceRepository.findAll(spec, pageable).map(this::toWithUserDTO);
    }

    /** Delete all records for a specific date and return count of deleted records. */
    @Transactional
    public long deleteByDate(LocalDate date) {
        long count = attendanceRepository.countByDate(date);
        attendanceRepository.deleteByDate(date);
        return count;
    }

    /** Dashboard summary: totals and today's attendance stats. Weekends and Peru holidays always report all-zero — attendance isn't tracked those days. */
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        LocalDate today = LocalDate.now();
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY || PeruHolidays.isHoliday(today)) {
            return new DashboardSummaryDTO(0, 0, 0, 0);
        }

        long totalRecords  = attendanceRepository.count();
        long presentToday  = attendanceRepository.countByStatusAndDate(AttendanceRecord.Status.Present, today);
        long lateToday     = attendanceRepository.countByStatusAndDate(AttendanceRecord.Status.Late, today);
        long totalTeachers = userRepository.countByRole(User.Role.TEACHER);
        long absentToday   = Math.max(0, totalTeachers - presentToday - lateToday);

        return new DashboardSummaryDTO(totalRecords, presentToday, lateToday, absentToday);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private AttendanceRecordResponseDTO toDTO(AttendanceRecord r) {
        AttendanceRecordResponseDTO dto = new AttendanceRecordResponseDTO();
        dto.setId(r.getId());
        dto.setDate(r.getDate());
        dto.setDayOfWeek(DayOfWeekEs.label(r.getDate()));
        dto.setTimeIn(r.getTimeIn());
        dto.setTimeOut(r.getTimeOut());
        dto.setStatus(r.getStatus().name());
        dto.setNotes(r.getNotes());
        return dto;
    }

    private AttendanceRecordWithUserDTO toWithUserDTO(AttendanceRecord r) {
        return new AttendanceRecordWithUserDTO(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getFullName(),
                r.getDate(),
                DayOfWeekEs.label(r.getDate()),
                r.getTimeIn(),
                r.getTimeOut(),
                r.getStatus().name(),
                r.getNotes()
        );
    }
}
