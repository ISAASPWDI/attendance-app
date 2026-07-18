package com.attendance.demo.dto.attendances;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryDTO {
    private long totalRecords;
    private long presentToday;
    private long lateToday;
    private long absentToday;
}
