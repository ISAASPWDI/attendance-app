package com.attendance.demo.exception.users;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("El usuario con id " + id + " no fue encontrado");
    }

    public UserNotFoundException(String username) {
        super("El usuario con username '" + username + "' no fue encontrado");
    }
}
