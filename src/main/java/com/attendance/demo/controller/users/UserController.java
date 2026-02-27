package com.attendance.demo.controller.users;
import com.attendance.demo.dto.filter.UserFilter;
import com.attendance.demo.dto.users.UserDetailDTO;
import com.attendance.demo.service.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping()
    public ResponseEntity<Page<UserDetailDTO>> getUsers(
            UserFilter userFilter,
            Pageable pageable){
        Page<UserDetailDTO> users = this.userService.getUsersAndAttendanceRecords(userFilter,pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailDTO> getUserById (@PathVariable Long userId) {
        UserDetailDTO user = this.userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

}
