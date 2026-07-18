package com.attendance.demo.exception.auth;

public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException() {
        super("Código inválido o expirado");
    }
}
