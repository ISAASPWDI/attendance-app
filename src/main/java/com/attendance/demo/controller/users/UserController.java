package com.attendance.demo.controller.users;
import com.attendance.demo.dto.users.UserDetailDTO;
import com.attendance.demo.entity.User;
import com.attendance.demo.service.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping
    public List<User> getUsers(){
        return this.userService.getUsers();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailDTO> getUserById (@PathVariable Long userId) {
        UserDetailDTO user = this.userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

}
