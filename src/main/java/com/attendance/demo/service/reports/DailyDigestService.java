package com.attendance.demo.service.reports;

import com.attendance.demo.entity.AttendanceRecord;
import com.attendance.demo.entity.User;
import com.attendance.demo.repository.AttendanceRepository;
import com.attendance.demo.repository.UserRepository;
import com.attendance.demo.service.email.EmailService;
import com.attendance.demo.util.DayOfWeekEs;
import com.attendance.demo.util.PeruHolidays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Emails every DIRECTOR a same-day attendance summary, split by role and status. Weekdays only. */
@Service
public class DailyDigestService {

    private static final Logger log = LoggerFactory.getLogger(DailyDigestService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 30 14 * * MON-FRI", zone = "America/Lima")
    public void sendDailyDigest() {
        LocalDate today = LocalDate.now();
        if (PeruHolidays.isHoliday(today)) {
            return;
        }
        String dateLabel = DayOfWeekEs.label(today) + " " + today.format(DATE_FORMAT);

        long totalToday = attendanceRepository.countByDate(today);

        long presentTeacher = attendanceRepository.countByStatusAndDateAndUser_Role(AttendanceRecord.Status.Present, today, User.Role.TEACHER);
        long lateTeacher = attendanceRepository.countByStatusAndDateAndUser_Role(AttendanceRecord.Status.Late, today, User.Role.TEACHER);
        long totalTeachers = userRepository.countByRole(User.Role.TEACHER);
        long absentTeacher = Math.max(0, totalTeachers - presentTeacher - lateTeacher);

        long presentDirector = attendanceRepository.countByStatusAndDateAndUser_Role(AttendanceRecord.Status.Present, today, User.Role.DIRECTOR);
        long lateDirector = attendanceRepository.countByStatusAndDateAndUser_Role(AttendanceRecord.Status.Late, today, User.Role.DIRECTOR);
        long totalDirectors = userRepository.countByRole(User.Role.DIRECTOR);
        long absentDirector = Math.max(0, totalDirectors - presentDirector - lateDirector);

        for (User director : userRepository.findAllByRoleAndEmailIsNotNull(User.Role.DIRECTOR)) {
            try {
                emailService.sendDailyDigest(director.getEmail(), director.getFullName(), dateLabel,
                        totalToday,
                        presentTeacher, lateTeacher, absentTeacher,
                        presentDirector, lateDirector, absentDirector);
            } catch (Exception e) {
                log.warn("No se pudo enviar el resumen diario a {}: {}", director.getEmail(), e.getMessage());
            }
        }
    }
}
