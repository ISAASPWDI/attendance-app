package com.attendance.demo.exception.attendances;

import java.time.LocalDate;

public class RecordAlreadyExistsException extends RuntimeException {

    public RecordAlreadyExistsException(LocalDate date) {
        super("Ya existe un registro de asistencia para el " + date);
    }
}
