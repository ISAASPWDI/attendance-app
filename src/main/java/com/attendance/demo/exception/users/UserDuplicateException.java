package com.attendance.demo.exception.users;

public class UserDuplicateException extends RuntimeException{
    public UserDuplicateException ( String username ){
        super("El usuario " + username + " ya existe");

    }
}
