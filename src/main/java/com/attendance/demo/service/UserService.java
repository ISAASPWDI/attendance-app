package com.attendance.demo.service;
import com.attendance.demo.dto.users.LoginUserDTO;
import com.attendance.demo.dto.users.RegisterUserDTO;
import com.attendance.demo.entity.User;
import com.attendance.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    public BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final UserRepository userRepository;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JWTService jwtService;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
    public  User registerUser(RegisterUserDTO newUserInfo){
        User newUser = new User();
        newUser.setUsername(newUserInfo.getUsername());
        newUser.setPassword(encoder.encode(newUserInfo.getPassword()));
        newUser.setRole(newUserInfo.getRole());
        return this.userRepository.save(newUser);
    }

    public String verify(LoginUserDTO loginUserInfo) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserInfo.getUsername(), loginUserInfo.getPassword()));

        if ( authentication.isAuthenticated()) return JWTService.generateToken(loginUserInfo.getUsername());

        return "auth failed";
    }
}
