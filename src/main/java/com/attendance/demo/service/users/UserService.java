package com.attendance.demo.service.users;

import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import com.attendance.demo.dto.users.UserDetailDTO;
import com.attendance.demo.entity.User;
import com.attendance.demo.exception.users.UserNotFoundException;
import com.attendance.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public UserDetailDTO getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        List<AttendanceRecordDTO> attendanceDTOs = user.getAttendanceRecords()
                .stream()
                .map(record -> {
                    AttendanceRecordDTO dto = new AttendanceRecordDTO();
                    dto.setAttendanceId(record.getId());
                    dto.setDate(record.getDate());
                    dto.setTimeIn(record.getTimeIn());
                    dto.setTimeOut(record.getTimeOut());
                    dto.setStatus(record.getStatus().name());
                    dto.setNotes(record.getNotes());
                    return dto;
                })
                .toList();

        return new UserDetailDTO(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                attendanceDTOs
        );
    }
    public Optional<User> findUserByUsername(String username){
        return this.userRepository.findByUsername(username);
    }
}
