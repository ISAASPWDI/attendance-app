package com.attendance.demo.exception.attendances;

import java.time.LocalTime;

public class RecordTimeInWindowException extends RuntimeException {

    public RecordTimeInWindowException(LocalTime timeIn) {
        super("La hora de entrada " + timeIn + " debe estar entre las 7:30 am y las 9:00 am");
    }

}
