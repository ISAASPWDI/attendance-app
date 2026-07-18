package com.attendance.demo.exception.auth;

import com.attendance.demo.exception.ErrorResponse;
import com.attendance.demo.service.email.EmailSendException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Order(1)
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCode(InvalidVerificationCodeException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid verification code", ex, request);
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyVerified(EmailAlreadyVerifiedException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Email already verified", ex, request);
    }

    @ExceptionHandler(VerificationCooldownException.class)
    public ResponseEntity<ErrorResponse> handleCooldown(VerificationCooldownException ex, WebRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Verification cooldown", ex, request);
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ErrorResponse> handleEmailSend(EmailSendException ex, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery failed", ex, request);
    }

    /** Thrown by Spring Security's authentication manager when CustomUserDetails.isEnabled() is false. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Account not verified",
                "Debes verificar tu correo electrónico antes de iniciar sesión",
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, RuntimeException ex, WebRequest request) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                error,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, status);
    }
}
