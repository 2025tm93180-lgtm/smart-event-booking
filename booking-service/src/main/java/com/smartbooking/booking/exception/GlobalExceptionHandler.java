
package com.smartbooking.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex) {

        HttpStatus status = HttpStatus.valueOf(
                ex.getStatusCode().value());

        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "message", ex.getReason() == null
                                ? status.getReasonPhrase()
                                : ex.getReason()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage() == null
                                ? "Invalid request"
                                : ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed");

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "message", message
                ));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotencyConflict(
            IdempotencyConflictException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 409,
                        "error", "Conflict",
                        "message", ex.getMessage()
                ));
    }

}
