package com.attendance.demo.service.users;

import com.attendance.demo.dto.attendances.AttendanceRecordResponseDTO;
import com.attendance.demo.dto.filter.UserFilter;
import com.attendance.demo.dto.users.UpdateUserDTO;
import com.attendance.demo.dto.users.UserDetailDTO;
import com.attendance.demo.entity.User;
import com.attendance.demo.exception.users.UserNotFoundException;
import com.attendance.demo.repository.UserRepository;
import com.attendance.demo.specification.UserSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserDetailDTO> getUsersAndAttendanceRecords(UserFilter userFilter, Pageable pageable) {
        Specification<User> spec = UserSpecification.filter(userFilter);
        return userRepository.findAll(spec, pageable).map(this::toDetailDTO);
    }

    @Transactional(readOnly = true)
    public UserDetailDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toDetailDTO(user);
    }

    @Transactional
    public void updateSignatureUrl(Long userId, String url) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setSignatureUrl(url);
        userRepository.save(user);
    }

    @Transactional
    public void updateFingerprintUrl(Long userId, String url) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setFingerprintUrl(url);
        userRepository.save(user);
    }

    @Transactional
    public void updatePhotoUrl(Long userId, String url) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setPhotoUrl(url);
        userRepository.save(user);
    }

    @Transactional
    public UserDetailDTO updateUser(Long userId, UpdateUserDTO dto, boolean requesterIsDirector) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (requesterIsDirector && dto.getRole() != null) user.setRole(dto.getRole());

        return toDetailDTO(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        userRepository.deleteById(userId);
    }

    private UserDetailDTO toDetailDTO(User user) {
        List<AttendanceRecordResponseDTO> records = user.getAttendanceRecords() == null
                ? List.of()
                : user.getAttendanceRecords().stream()
                .map(r -> new AttendanceRecordResponseDTO(
                        r.getId(),
                        r.getDate(),
                        r.getTimeIn(),
                        r.getTimeOut(),
                        r.getStatus().name(),
                        r.getNotes()
                ))
                .toList();

        return new UserDetailDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getPhotoUrl(),
                records
        );
    }
}
