package com.attendance.demo.dto.auth;


import com.attendance.demo.dto.users.UserResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String status;
    private String accessToken;
    private String refreshToken;
    private UserResponseDTO user;
}
