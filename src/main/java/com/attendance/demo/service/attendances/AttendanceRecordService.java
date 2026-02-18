package com.attendance.demo.service.attendances;

import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import com.attendance.demo.entity.AttendanceRecord;
import com.attendance.demo.exception.attendances.RecordException;
import com.attendance.demo.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AttendanceRecordService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Transactional
    public AttendanceRecord create(AttendanceRecordDTO attendanceInfo) {
        Optional<AttendanceRecord> existingRecord = this.attendanceRepository.findById(attendanceInfo.getAttendanceId());
        if ( existingRecord.isPresent() ) throw new RecordException(attendanceInfo.getAttendanceId());

        AttendanceRecord newRecord = new AttendanceRecord();
        newRecord.setTimeIn(attendanceInfo.getTimeIn());
        newRecord.setTimeOut(attendanceInfo.getTimeOut());
        newRecord.setStatus(AttendanceRecord.Status.valueOf(attendanceInfo.getStatus()));
        newRecord.setNotes(attendanceInfo.getNotes());

        return this.attendanceRepository.save(newRecord);
    }

    @Transactional( readOnly = true )
    public List<AttendanceRecord> getAll() {
        return this.attendanceRepository.findAll();
    }
}
