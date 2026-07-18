package com.attendance.demo.exception.attendances;

import java.time.LocalTime;

public class RecordTimeOutException extends RuntimeException{
    public RecordTimeOutException(LocalTime timeOut ){
        super("El tiempo " + timeOut + " de salida no debe ser antes al tiempo de entrada" );
    }
}
