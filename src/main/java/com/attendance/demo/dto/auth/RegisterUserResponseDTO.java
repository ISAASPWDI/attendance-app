package com.attendance.demo.dto.auth;


import com.attendance.demo.dto.users.UserResponseDTO;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RegisterUserResponseDTO {

    public Boolean success;
    public String message;
    public UserResponseDTO user;

}
