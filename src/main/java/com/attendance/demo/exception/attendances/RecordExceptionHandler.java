package com.attendance.demo.exception.attendances;

import com.attendance.demo.exception.ErrorResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Order(2)
public class RecordExceptionHandler {

    @ExceptionHandler(RecordTimeInException.class)
    public ResponseEntity<ErrorResponse> handleRecordTimeIn(RecordTimeInException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Hora de entrada inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(RecordTimeOutException.class)
    public ResponseEntity<ErrorResponse> handleRecordTimeOut(RecordTimeOutException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Hora de salida inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(RecordTimeInWindowException.class)
    public ResponseEntity<ErrorResponse> handleRecordTimeInWindow(RecordTimeInWindowException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Fuera de horario de entrada", ex.getMessage(), request);
    }

    @ExceptionHandler(RecordTimeOutWindowException.class)
    public ResponseEntity<ErrorResponse> handleRecordTimeOutWindow(RecordTimeOutWindowException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Fuera de horario de salida", ex.getMessage(), request);
    }

    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRecordNotFound(RecordNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "Registro no encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(RecordAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleRecordAlreadyExists(RecordAlreadyExistsException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, "Registro ya existe", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Operación no permitida", ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, WebRequest request) {
        ErrorResponse err = new ErrorResponse(
                status.value(), error, message,
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(err, status);
    }
}
