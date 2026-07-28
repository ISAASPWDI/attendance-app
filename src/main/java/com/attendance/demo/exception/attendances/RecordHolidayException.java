package com.attendance.demo.exception.attendances;

import java.time.LocalDate;

public class RecordHolidayException extends RuntimeException {

    public RecordHolidayException(LocalDate date, String holidayName) {
        super("No se puede registrar asistencia el " + date + ": es feriado (" + holidayName + ")");
    }

}
