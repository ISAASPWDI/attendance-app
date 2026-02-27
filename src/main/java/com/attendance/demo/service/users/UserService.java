package com.attendance.demo.service.users;
import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import com.attendance.demo.dto.filter.UserFilter;
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
import java.util.List;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;


//    public Pageable buildPageable(UserFilter userFilter, int page, int size){
//
//
//        Sort sort = Sort.unsorted();
//
//        //valor por defecto en caso no cuumplan los conmdicionales
//        Sort.Direction direction = Sort.Direction.ASC;
//
//        if ( userFilter.getOrder() != null){
//            direction = switch ( userFilter.getOrder() ){
//                case NEWEST -> Sort.Direction.ASC;
//                case OLDEST -> Sort.Direction.DESC;
//            };
//
//        }
//
//        if ( userFilter.getSortBy() != null ) {
//            String sortBy = switch ( userFilter.getSortBy() ) {
//                case Date -> "date";
//                case Name -> "username";
//                case Status -> "status";
//            };
//            sort = Sort.by(direction, sortBy);
//        }
//        // ejemplo de como quedan los datos
//        /*
//        page = 0
//        size = 5
//        sort = username ASC
//        */
//        return PageRequest.of(page, size, sort);
//    }

    public Page<UserDetailDTO> getUsersAndAttendanceRecords(UserFilter userFilter, Pageable pageable) {

        Specification<User> spec = UserSpecification.filter(userFilter);

        return this.userRepository.findAll(spec, pageable).map(( user ) -> new UserDetailDTO(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getAttendanceRecords().stream().map(
                        ( attendanceRecord) ->
                                new AttendanceRecordDTO(
                                        attendanceRecord.getId(),
                                        attendanceRecord.getDate(),
                                        attendanceRecord.getTimeIn(),
                                        attendanceRecord.getTimeOut(),
                                        attendanceRecord.getStatus().name(),
                                        attendanceRecord.getNotes()
                                )
                ).toList()
        ) );
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
//    public Optional<User> findUserByUsername(String username){
//        return this.userRepository.findByUsername(username);
//    }
}
