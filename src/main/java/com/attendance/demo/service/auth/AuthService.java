package com.attendance.demo.service.auth;

import com.attendance.demo.dto.auth.LoginResponseDTO;
import com.attendance.demo.dto.auth.LoginUserDTO;
import com.attendance.demo.dto.auth.RegisterUserDTO;
import com.attendance.demo.dto.auth.RegisterUserResponseDTO;
import com.attendance.demo.dto.users.UserResponseDTO;
import com.attendance.demo.entity.PendingRegistration;
import com.attendance.demo.entity.User;
import com.attendance.demo.entity.VerificationCode;
import com.attendance.demo.exception.auth.EmailAlreadyVerifiedException;
import com.attendance.demo.exception.auth.InvalidVerificationCodeException;
import com.attendance.demo.exception.auth.VerificationCooldownException;
import com.attendance.demo.exception.users.UserDuplicateException;
import com.attendance.demo.exception.users.UserNotFoundException;
import com.attendance.demo.repository.PendingRegistrationRepository;
import com.attendance.demo.repository.UserRepository;
import com.attendance.demo.repository.VerificationCodeRepository;
import com.attendance.demo.service.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final long CODE_VALID_MINUTES = 15;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private final BCryptPasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public AuthService(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    VerificationCodeRepository verificationCodeRepository;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JWTService jwtService;

    @Autowired
    EmailService emailService;

    /**
     * Registration never creates a User row — only a pending record with a verification code.
     * The account is created for real in {@link #verifyEmail} once the code is confirmed.
     */
    @Transactional
    public RegisterUserResponseDTO registerUser(RegisterUserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UserDuplicateException(dto.getUsername());
        }

        // A retry before verifying replaces the previous pending record with a fresh code.
        // Flush is required: Hibernate flushes inserts before deletes within the same transaction,
        // so without it this delete would run after the insert below and collide with the
        // username unique constraint.
        pendingRegistrationRepository.deleteByUsername(dto.getUsername());
        pendingRegistrationRepository.flush();

        PendingRegistration pending = new PendingRegistration();
        pending.setUsername(dto.getUsername());
        pending.setPasswordHash(encoder.encode(dto.getPassword()));
        pending.setEmail(dto.getEmail());
        pending.setFirstName(dto.getFirstName());
        pending.setLastName(dto.getLastName());
        pending.setRole(dto.getRole());
        pending.setCode(generateCode());
        pending.setCreatedAt(LocalDateTime.now());
        pending.setExpiresAt(pending.getCreatedAt().plusMinutes(CODE_VALID_MINUTES));
        pendingRegistrationRepository.save(pending);

        // Email delivery failures never block registration; the user can always resend later.
        try {
            emailService.sendVerificationCode(pending.getEmail(), pending.getCode());
        } catch (Exception ignored) {
        }

        return new RegisterUserResponseDTO(
                true,
                "Revisa tu correo para verificar tu cuenta.",
                new UserResponseDTO(null, dto.getUsername(), dto.getFirstName(), dto.getLastName(), dto.getRole().name())
        );
    }

    public LoginResponseDTO verify(LoginUserDTO loginInfo) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginInfo.getUsername(), loginInfo.getPassword())
        );

        User user = userRepository.findByUsername(loginInfo.getUsername())
                .orElseThrow(() -> new UserNotFoundException(loginInfo.username));

        return new LoginResponseDTO(
                "Login exitoso!",
                JWTService.generateAccessToken(user.getUsername()),
                JWTService.generateRefreshToken(user.getUsername()),
                toResponseDTO(user)
        );
    }

    /** Confirms the code and only now creates the real, already-verified User account. */
    @Transactional
    public void verifyEmail(String username, String code) {
        if (userRepository.existsByUsername(username)) {
            throw new EmailAlreadyVerifiedException();
        }

        PendingRegistration pending = pendingRegistrationRepository.findByUsername(username)
                .orElseThrow(InvalidVerificationCodeException::new);

        if (pending.isExpired() || !pending.matches(code)) {
            throw new InvalidVerificationCodeException();
        }

        userRepository.save(pending.toVerifiedUser());
        pendingRegistrationRepository.deleteByUsername(username);
    }

    @Transactional
    public void resendVerificationCode(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new EmailAlreadyVerifiedException();
        }

        PendingRegistration pending = pendingRegistrationRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (pending.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
            throw new VerificationCooldownException();
        }

        pending.setCode(generateCode());
        pending.setCreatedAt(LocalDateTime.now());
        pending.setExpiresAt(pending.getCreatedAt().plusMinutes(CODE_VALID_MINUTES));
        pendingRegistrationRepository.save(pending);

        emailService.sendVerificationCode(pending.getEmail(), pending.getCode());
    }

    @Transactional
    public void forgotPassword(String username) {
        // Always succeeds regardless of whether the username exists (or has an email on file),
        // to avoid leaking registered accounts.
        userRepository.findByUsername(username)
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .ifPresent(this::issuePasswordResetCode);
    }

    @Transactional
    public void resetPassword(String username, String code, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidVerificationCodeException::new);

        VerificationCode verificationCode = verificationCodeRepository
                .findByUserIdAndPurpose(user.getId(), VerificationCode.Purpose.PASSWORD_RESET)
                .orElseThrow(InvalidVerificationCodeException::new);

        if (verificationCode.isUsed() || verificationCode.isExpired() || !verificationCode.matches(code)) {
            throw new InvalidVerificationCodeException();
        }
        verificationCode.markUsed();
        verificationCodeRepository.save(verificationCode);

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }

    private void issuePasswordResetCode(User user) {
        verificationCodeRepository.findByUserIdAndPurpose(user.getId(), VerificationCode.Purpose.PASSWORD_RESET)
                .ifPresent(existing -> {
                    if (existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
                        throw new VerificationCooldownException();
                    }
                });
        verificationCodeRepository.deleteByUserIdAndPurpose(user.getId(), VerificationCode.Purpose.PASSWORD_RESET);
        // Hibernate flushes inserts before deletes within the same transaction by default, so without
        // an explicit flush here the delete above would run *after* the insert below and collide with
        // the (user_id, purpose) unique constraint.
        verificationCodeRepository.flush();

        String code = generateCode();
        VerificationCode verificationCode = VerificationCode.create(
                user.getId(), code, VerificationCode.Purpose.PASSWORD_RESET, CODE_VALID_MINUTES);
        verificationCodeRepository.save(verificationCode);

        emailService.sendPasswordResetCode(user.getEmail(), code);
    }

    private String generateCode() {
        return String.format("%04d", random.nextInt(10000));
    }

    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }
}
