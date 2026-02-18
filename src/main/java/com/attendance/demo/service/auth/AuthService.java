package com.attendance.demo.service.auth;
import com.attendance.demo.dto.attendances.AttendanceRecordDTO;
import com.attendance.demo.dto.auth.LoginResponseDTO;
import com.attendance.demo.dto.auth.LoginUserDTO;
import com.attendance.demo.dto.auth.RegisterUserDTO;
import com.attendance.demo.dto.auth.RegisterUserResponseDTO;
import com.attendance.demo.dto.users.*;
import com.attendance.demo.entity.User;
import com.attendance.demo.exception.users.UserDuplicateException;
import com.attendance.demo.exception.users.UserNotFoundException;
import com.attendance.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.stereotype.Service;


@Service
public class AuthService {

    private final BCryptPasswordEncoder encoder;
    public AuthService(BCryptPasswordEncoder encoder){
        this.encoder = encoder;
    }

    @Autowired
    UserRepository userRepository;

//    @Autowired
//    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JWTService jwtService;



    //put this method into users controller

    //put this method into users controller

    //put this method into users controller


    public RegisterUserResponseDTO registerUser(RegisterUserDTO newUserInfo){
        if ( this.userRepository.existsByUsername(newUserInfo.getUsername()) ) {
            throw new UserDuplicateException(newUserInfo.getUsername());
        }

        User user = new User();
        user.setUsername(newUserInfo.getUsername());
        user.setPassword(this.encoder.encode(newUserInfo.getPassword()));
        user.setRole(newUserInfo.getRole());

        this.userRepository.save(user);
        UserResponseDTO userResponse = new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
        return new RegisterUserResponseDTO(
                true,
                "Usuario registrado correctamente",
                userResponse
        );
    }

    public LoginResponseDTO verify(LoginUserDTO loginUserInfo) {

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserInfo.getUsername(), loginUserInfo.getPassword()));

        User user = this.userRepository.findByUsername(loginUserInfo.getUsername())
                .orElseThrow(() -> new UserNotFoundException(loginUserInfo.username));
        UserResponseDTO userResponse = new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );


        return new LoginResponseDTO(
                "Login exitoso!",
                JWTService.generateAccessToken(user.getUsername()),
                JWTService.generateRefreshToken(user.getUsername()),
                userResponse
        );



    }
}
