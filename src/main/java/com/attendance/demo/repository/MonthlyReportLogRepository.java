package com.attendance.demo.repository;

import com.attendance.demo.entity.MonthlyReportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MonthlyReportLogRepository extends JpaRepository<MonthlyReportLog, Long> {

    boolean existsByPeriod(LocalDate period);

    List<MonthlyReportLog> findAllByDeliveredFalse();
}
