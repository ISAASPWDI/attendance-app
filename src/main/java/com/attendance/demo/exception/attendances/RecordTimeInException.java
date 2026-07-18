package com.attendance.demo.exception.attendances;

import java.time.LocalTime;

public class RecordTimeInException extends RuntimeException{

    public RecordTimeInException(LocalTime timeIn){
        super("El tiempo " + timeIn + " de entrada no debe ser después al tiempo de salida" );
    }

}
