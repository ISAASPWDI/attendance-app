package com.attendance.demo.controller.users;

import com.attendance.demo.config.CustomUserDetails;
import com.attendance.demo.dto.auth.RegisterUserDTO;
import com.attendance.demo.dto.auth.RegisterUserResponseDTO;
import com.attendance.demo.dto.filter.UserFilter;
import com.attendance.demo.dto.users.CreateUserDTO;
import com.attendance.demo.dto.users.UpdateUserDTO;
import com.attendance.demo.dto.users.UserDetailDTO;
import com.attendance.demo.entity.User;
import com.attendance.demo.exception.users.SelfDeleteException;
import com.attendance.demo.service.auth.AuthService;
import com.attendance.demo.service.cloud.CloudinaryService;
import com.attendance.demo.service.users.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    AuthService authService;

    @Autowired
    CloudinaryService cloudinaryService;

    @GetMapping
    public ResponseEntity<Page<UserDetailDTO>> getUsers(UserFilter userFilter, Pageable pageable) {
        return ResponseEntity.ok(userService.getUsersAndAttendanceRecords(userFilter, pageable));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * Director-initiated user creation goes through the same pending-registration + email
     * verification flow as public self-registration — an account is never created unverified,
     * regardless of who requests it.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('DIRECTOR')")
    public ResponseEntity<RegisterUserResponseDTO> createUser(@RequestBody @Valid CreateUserDTO dto) {
        RegisterUserDTO registerDto = new RegisterUserDTO();
        registerDto.setUsername(dto.getUsername());
        registerDto.setPassword(dto.getPassword());
        registerDto.setEmail(dto.getEmail());
        registerDto.setFirstName(dto.getFirstName());
        registerDto.setLastName(dto.getLastName());
        registerDto.setRole(dto.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(registerDto));
    }

    /** Update name (owner or DIRECTOR); role changes are only honored when the requester is DIRECTOR. */
    @PatchMapping("/{userId}")
    public ResponseEntity<UserDetailDTO> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!isOwnerOrDirector(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean isDirector = userDetails.user.getRole() == User.Role.DIRECTOR;
        return ResponseEntity.ok(userService.updateUser(userId, dto, isDirector));
    }

    /** DIRECTOR can delete any other user; anyone (TEACHER or DIRECTOR) can delete their own account, except a DIRECTOR deleting themself. */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isSelf = userDetails.user.getId().equals(userId);
        boolean isDirector = userDetails.user.getRole() == User.Role.DIRECTOR;

        if (!isSelf && !isDirector) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (isSelf && isDirector) {
            throw new SelfDeleteException();
        }
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /** Upload teacher's signature image. Teachers can only update their own; DIRECTOR can update any. */
    @PostMapping("/{userId}/signature")
    public ResponseEntity<Map<String, String>> uploadSignature(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws Exception {
        if (!isOwnerOrDirector(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String url = cloudinaryService.uploadImage(file, "signatures", "signature_" + userId);
        userService.updateSignatureUrl(userId, url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Upload teacher's fingerprint image. Teachers can only update their own; DIRECTOR can update any. */
    @PostMapping("/{userId}/fingerprint")
    public ResponseEntity<Map<String, String>> uploadFingerprint(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws Exception {
        if (!isOwnerOrDirector(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String url = cloudinaryService.uploadImage(file, "fingerprints", "fingerprint_" + userId);
        userService.updateFingerprintUrl(userId, url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Upload profile photo. Teachers/directors can only update their own; DIRECTOR can update any. */
    @PostMapping("/{userId}/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws Exception {
        if (!isOwnerOrDirector(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String url = cloudinaryService.uploadImage(file, "photos", "photo_" + userId);
        userService.updatePhotoUrl(userId, url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private boolean isOwnerOrDirector(Long targetUserId, CustomUserDetails userDetails) {
        return userDetails.user.getRole() == User.Role.DIRECTOR
                || userDetails.user.getId().equals(targetUserId);
    }
}
