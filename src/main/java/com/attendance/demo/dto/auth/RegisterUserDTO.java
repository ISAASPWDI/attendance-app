package com.attendance.demo.dto.auth;

import com.attendance.demo.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterUserDTO {

    @NotNull
    public String username;

    @NotNull
    public String password;

    @NotNull
    public String email;

    public String firstName;

    public String lastName;

    public User.Role role = User.Role.TEACHER;
}
