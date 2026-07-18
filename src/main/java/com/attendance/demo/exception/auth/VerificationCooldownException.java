package com.attendance.demo.exception.auth;

public class VerificationCooldownException extends RuntimeException {
    public VerificationCooldownException() {
        super("Espera unos segundos antes de solicitar otro código");
    }
}
