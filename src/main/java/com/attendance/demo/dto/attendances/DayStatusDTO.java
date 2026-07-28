package com.attendance.demo.dto.attendances;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DayStatusDTO {
    private boolean holiday;
    private String holidayName;
    private boolean weekend;
}
