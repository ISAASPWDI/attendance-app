package com.attendance.demo.service;
import com.attendance.demo.dto.CreateUserDTO;
import com.attendance.demo.entity.User;
import com.attendance.demo.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    public BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
    public  User createUser(CreateUserDTO newUserInfo){
        User newUser = new User();
        newUser.setUsername(newUserInfo.getUsername());
        newUser.setPassword(encoder.encode(newUserInfo.getPassword()));
        newUser.setRole(newUserInfo.getRole());
        return this.userRepository.save(newUser);
    }
}
