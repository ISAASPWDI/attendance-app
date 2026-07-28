package com.attendance.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_report_log", uniqueConstraints = @UniqueConstraint(columnNames = "period"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MonthlyReportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate period;

    @Column(name = "excel_url", length = 500, nullable = false)
    private String excelUrl;

    @Column(name = "pdf_url", length = 500, nullable = false)
    private String pdfUrl;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private boolean delivered = false;
}
