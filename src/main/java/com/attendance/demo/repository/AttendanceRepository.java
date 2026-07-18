package com.attendance.demo.repository;

import com.attendance.demo.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<AttendanceRecord, Long>, JpaSpecificationExecutor<AttendanceRecord> {

    Optional<AttendanceRecord> findByUserIdAndDate(Long userId, LocalDate date);

    long countByDate(LocalDate date);

    long countByStatusAndDate(AttendanceRecord.Status status, LocalDate date);

    void deleteByDate(LocalDate date);
}
