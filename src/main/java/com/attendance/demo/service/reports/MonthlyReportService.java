package com.attendance.demo.service.reports;

import com.attendance.demo.dto.filter.AttendanceFilter;
import com.attendance.demo.entity.MonthlyReportLog;
import com.attendance.demo.entity.User;
import com.attendance.demo.repository.AttendanceRepository;
import com.attendance.demo.repository.MonthlyReportLogRepository;
import com.attendance.demo.repository.UserRepository;
import com.attendance.demo.service.attendances.ReportService;
import com.attendance.demo.service.cloud.CloudinaryService;
import com.attendance.demo.service.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generates and emails the previous month's attendance report, then purges that month's
 * records — only once the report has actually reached at least one director. Delivery that
 * fails (transient EmailJS outage) leaves the log row undelivered so the next monthly run
 * retries it before generating the new month's report.
 */
@Service
public class MonthlyReportService {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportService.class);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es"));

    @Autowired
    private ReportService reportService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MonthlyReportLogRepository monthlyReportLogRepository;

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 0 1 1 * *", zone = "America/Lima")
    @Transactional
    public void runMonthlyReportCycle() {
        retryUndeliveredReports();
        generateCurrentCycleReport();
    }

    private void retryUndeliveredReports() {
        for (MonthlyReportLog reportLog : monthlyReportLogRepository.findAllByDeliveredFalse()) {
            String monthLabel = YearMonth.from(reportLog.getPeriod()).format(MONTH_LABEL);
            if (attemptDelivery(reportLog.getExcelUrl(), reportLog.getPdfUrl(), monthLabel)) {
                reportLog.setDelivered(true);
                monthlyReportLogRepository.save(reportLog);
                purgeMonth(reportLog.getPeriod());
            }
        }
    }

    private void generateCurrentCycleReport() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate from = previousMonth.atDay(1);
        LocalDate to = previousMonth.atEndOfMonth();

        if (monthlyReportLogRepository.existsByPeriod(from)) {
            return;
        }

        AttendanceFilter filter = new AttendanceFilter();
        filter.setFromDate(from);
        filter.setToDate(to);

        String monthLabel = previousMonth.format(MONTH_LABEL);
        String excelUrl;
        String pdfUrl;
        try {
            byte[] excelBytes = reportService.generateExcel(filter);
            byte[] pdfBytes = reportService.generatePdf(filter);
            excelUrl = cloudinaryService.uploadRaw(excelBytes, "monthly-reports", "asistencias_" + from);
            pdfUrl = cloudinaryService.uploadRaw(pdfBytes, "monthly-reports", "asistencias_" + from + "_pdf");
        } catch (Exception e) {
            log.error("No se pudo generar/subir el reporte mensual de {}: {}", monthLabel, e.getMessage(), e);
            return;
        }

        MonthlyReportLog reportLog = new MonthlyReportLog();
        reportLog.setPeriod(from);
        reportLog.setExcelUrl(excelUrl);
        reportLog.setPdfUrl(pdfUrl);
        reportLog.setGeneratedAt(LocalDateTime.now());
        reportLog.setDelivered(false);
        monthlyReportLogRepository.save(reportLog);

        if (attemptDelivery(excelUrl, pdfUrl, monthLabel)) {
            reportLog.setDelivered(true);
            monthlyReportLogRepository.save(reportLog);
            purgeMonth(from);
        }
    }

    private boolean attemptDelivery(String excelUrl, String pdfUrl, String monthLabel) {
        boolean anySent = false;
        for (User director : userRepository.findAllByRoleAndEmailIsNotNull(User.Role.DIRECTOR)) {
            try {
                emailService.sendMonthlyReportReady(director.getEmail(), director.getFullName(), monthLabel, excelUrl, pdfUrl);
                anySent = true;
            } catch (Exception e) {
                log.warn("No se pudo enviar el reporte mensual a {}: {}", director.getEmail(), e.getMessage());
            }
        }
        return anySent;
    }

    private void purgeMonth(LocalDate period) {
        attendanceRepository.deleteByDateBetween(period, YearMonth.from(period).atEndOfMonth());
    }
}
