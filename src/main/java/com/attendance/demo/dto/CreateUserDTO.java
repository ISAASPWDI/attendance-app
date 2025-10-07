package com.attendance.demo.dto;

import com.attendance.demo.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateUserDTO {

    public String username;
    public String password;
    public User.Role role = User.Role.valueOf("TEACHER");

}
