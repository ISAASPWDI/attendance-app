package com.attendance.demo.exception.attendances;

import java.time.LocalTime;

public class RecordTimeOutWindowException extends RuntimeException {

    public RecordTimeOutWindowException(LocalTime timeOut) {
        super("La hora de salida " + timeOut + " debe estar entre la 1:00 pm y las 2:00 pm");
    }

}
