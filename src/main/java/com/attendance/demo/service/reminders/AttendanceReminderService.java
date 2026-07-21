package com.attendance.demo.service.reminders;

import com.attendance.demo.entity.User;
import com.attendance.demo.repository.AttendanceRepository;
import com.attendance.demo.repository.UserRepository;
import com.attendance.demo.service.email.EmailService;
import com.attendance.demo.util.DayOfWeekEs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Reminds users by email to register their entrada/salida while the respective window is still open.
 * Idempotent by construction: it re-derives "still pending" from the actual attendance record on every
 * run rather than tracking a separate "already emailed" flag, so it naturally stops once a user checks
 * in/out, and naturally does nothing outside the registration windows or on weekends.
 */
@Service
public class AttendanceReminderService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceReminderService.class);

    private static final LocalTime ENTRY_WINDOW_START = LocalTime.of(7, 30);
    private static final LocalTime ENTRY_WINDOW_END   = LocalTime.of(9, 0);
    private static final LocalTime EXIT_WINDOW_START  = LocalTime.of(13, 0);
    private static final LocalTime EXIT_WINDOW_END    = LocalTime.of(14, 0);

    private static final String ENTRY_WINDOW_LABEL = "7:30 am - 9:00 am";
    private static final String EXIT_WINDOW_LABEL  = "1:00 pm - 2:00 pm";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmailService emailService;

    /** Checks every 25 minutes; only acts on weekdays while an entrada/salida window is open. */
    @Scheduled(fixedRate = 25 * 60 * 1000)
    public void remindPendingAttendance() {
        LocalDate today = LocalDate.now();
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return;
        }

        LocalTime now = LocalTime.now();
        if (!now.isBefore(ENTRY_WINDOW_START) && !now.isAfter(ENTRY_WINDOW_END)) {
            remindEntry(today);
        } else if (!now.isBefore(EXIT_WINDOW_START) && !now.isAfter(EXIT_WINDOW_END)) {
            remindExit(today);
        }
    }

    private void remindEntry(LocalDate today) {
        String dateLabel = dateLabel(today);
        for (User user : userRepository.findAllByEmailIsNotNull()) {
            if (user.getEmail().isBlank()) continue;
            boolean alreadyCheckedIn = attendanceRepository.findByUserIdAndDate(user.getId(), today).isPresent();
            if (!alreadyCheckedIn) {
                sendReminder(user, "entrada", ENTRY_WINDOW_LABEL, dateLabel);
            }
        }
    }

    private void remindExit(LocalDate today) {
        String dateLabel = dateLabel(today);
        for (User user : userRepository.findAllByEmailIsNotNull()) {
            if (user.getEmail().isBlank()) continue;
            attendanceRepository.findByUserIdAndDate(user.getId(), today)
                    .filter(record -> record.getTimeOut() == null)
                    .ifPresent(record -> sendReminder(user, "salida", EXIT_WINDOW_LABEL, dateLabel));
        }
    }

    private String dateLabel(LocalDate date) {
        return DayOfWeekEs.label(date) + " " + date.format(DATE_FORMAT);
    }

    private void sendReminder(User user, String actionLabel, String windowLabel, String dateLabel) {
        try {
            emailService.sendAttendanceReminder(user.getEmail(), user.getFullName(), actionLabel, windowLabel, dateLabel);
        } catch (Exception e) {
            log.warn("No se pudo enviar el recordatorio de {} a {}: {}", actionLabel, user.getEmail(), e.getMessage());
        }
    }
}
