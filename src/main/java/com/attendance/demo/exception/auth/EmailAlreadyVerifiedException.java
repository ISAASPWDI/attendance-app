package com.attendance.demo.exception.auth;

public class EmailAlreadyVerifiedException extends RuntimeException {
    public EmailAlreadyVerifiedException() {
        super("El correo ya fue verificado");
    }
}
