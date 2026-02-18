package com.attendance.demo.controller.attendances;

import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import com.attendance.demo.entity.AttendanceRecord;
import com.attendance.demo.service.attendances.AttendanceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceRecordController {

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    @PostMapping
    public AttendanceRecord createAttendance(@RequestBody AttendanceRecordDTO attendanceInfo){
        return this.attendanceRecordService.create(attendanceInfo);
    }

    @GetMapping
    public List<AttendanceRecord> getAttendances() {
        return this.attendanceRecordService.getAll();
    }

}
