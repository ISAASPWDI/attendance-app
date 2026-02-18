package com.attendance.demo.exception.attendances;

import java.time.LocalTime;

public class RecordException extends RuntimeException{
    public RecordException(Long id){
        super("La asistencia " + id + " ya existe");
    }
    public RecordException(LocalTime time){
        super("Algo sucedió mal con el tiempo " + time + " especificado");
    }
}
