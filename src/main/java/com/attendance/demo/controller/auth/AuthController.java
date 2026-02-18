package com.attendance.demo.controller.auth;

import com.attendance.demo.dto.auth.LoginResponseDTO;
import com.attendance.demo.dto.auth.LoginUserDTO;
import com.attendance.demo.dto.auth.RegisterUserDTO;
import com.attendance.demo.dto.auth.RegisterUserResponseDTO;

import com.attendance.demo.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;



//    @GetMapping("/")
//    public String greeting (){
//        return "The API is working";
//    }


    // Put this method in users controller

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponseDTO> registerUser(@RequestBody RegisterUserDTO newUserInfo){
        RegisterUserResponseDTO newUser = this.authService.registerUser(newUserInfo);

        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginUserDTO loginUserInfo){
            LoginResponseDTO loginResponse = this.authService.verify(loginUserInfo);
            return ResponseEntity.ok(loginResponse);

    }
    // Put this method in users controller

}
