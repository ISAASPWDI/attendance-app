package com.attendance.demo.dto.users;

import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserDetailDTO {
    private Long id;
    private String username;
    private String role;
    private List<AttendanceRecordDTO> attendanceRecords;
}