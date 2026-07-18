package com.attendance.demo.controller.auth;

import com.attendance.demo.config.CustomUserDetails;
import com.attendance.demo.dto.auth.LoginResponseDTO;
import com.attendance.demo.dto.auth.LoginUserDTO;
import com.attendance.demo.dto.auth.RegisterUserDTO;
import com.attendance.demo.dto.auth.RegisterUserResponseDTO;
import com.attendance.demo.dto.auth.ResetPasswordDTO;
import com.attendance.demo.dto.auth.UsernameOnlyDTO;
import com.attendance.demo.dto.auth.VerifyEmailDTO;
import com.attendance.demo.dto.users.UserProfileDTO;
import com.attendance.demo.entity.User;
import com.attendance.demo.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponseDTO> registerUser(@RequestBody @Valid RegisterUserDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginUserDTO loginInfo) {
        return ResponseEntity.ok(authService.verify(loginInfo));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody @Valid VerifyEmailDTO dto) {
        authService.verifyEmail(dto.getUsername(), dto.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody @Valid UsernameOnlyDTO dto) {
        authService.resendVerificationCode(dto.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid UsernameOnlyDTO dto) {
        authService.forgotPassword(dto.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        authService.resetPassword(dto.getUsername(), dto.getCode(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /** Returns the authenticated user's profile. Used by the frontend navbar. */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User u = userDetails.user;
        return ResponseEntity.ok(new UserProfileDTO(
                u.getId(),
                u.getUsername(),
                u.getFirstName(),
                u.getLastName(),
                u.getRole().name(),
                u.getPhotoUrl()
        ));
    }
}
