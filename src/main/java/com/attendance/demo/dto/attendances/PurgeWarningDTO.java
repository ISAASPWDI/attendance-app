package com.attendance.demo.dto.attendances;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurgeWarningDTO {
    private boolean active;
    private Long daysRemaining;
    private LocalDate purgeDate;
}
