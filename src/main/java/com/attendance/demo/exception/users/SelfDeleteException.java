package com.attendance.demo.exception.users;

public class SelfDeleteException extends RuntimeException {
    public SelfDeleteException() {
        super("No puedes eliminar tu propia cuenta");
    }
}
