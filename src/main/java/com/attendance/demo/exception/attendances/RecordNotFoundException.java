package com.attendance.demo.exception.attendances;

public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(Long id) {
        super("No se encontró la asistencia con id: " + id);
    }

    public RecordNotFoundException(String message) {
        super(message);
    }
}
