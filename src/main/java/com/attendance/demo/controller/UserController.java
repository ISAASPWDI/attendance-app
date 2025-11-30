package com.attendance.demo.controller;

import com.attendance.demo.dto.users.LoginUserDTO;
import com.attendance.demo.dto.users.RegisterUserDTO;
import com.attendance.demo.entity.User;
import com.attendance.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    public UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/")
    public String hello(HttpServletRequest request){
        return "hello world your id is : " + request.getSession().getId();
    }
    @GetMapping("/users")
    public List<User> getUsers(){
        return this.userService.getUsers();
    }
    @PostMapping("/register")
    public User registerUser(@RequestBody RegisterUserDTO newUserInfo){
        return this.userService.registerUser(newUserInfo);
    }

    @PostMapping("/login")
    public String loginUser(@RequestBody LoginUserDTO loginUserInfo){
        return this.userService.verify(loginUserInfo);
    }
}
