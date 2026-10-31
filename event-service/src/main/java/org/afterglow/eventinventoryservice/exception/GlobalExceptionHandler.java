package org.afterglow.eventinventoryservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventException.class)
    public ResponseEntity<ErrorResponse> handleEventException(EventException exception) {
        log.error("Handling EventException: {}", exception.getMessage());
        HttpStatus status = exception.getHttpStatus() != null ? exception.getHttpStatus() : HttpStatus.BAD_REQUEST;
        ErrorResponse response = ErrorResponse.of(
                exception.getClass().getSimpleName(),
                status,
                exception.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(VenueException.class)
    public ResponseEntity<ErrorResponse> handleVenueException(VenueException exception) {
        log.error("Handling VenueException: {}", exception.getMessage());
        HttpStatus status = exception.getHttpStatus() != null ? exception.getHttpStatus() : HttpStatus.BAD_REQUEST;
        ErrorResponse response = ErrorResponse.of(
                exception.getClass().getSimpleName(),
                status,
                exception.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException exception) {
        log.error("Handling ResourceNotFoundException: {}", exception.getMessage());
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse response = ErrorResponse.of(
                exception.getClass().getSimpleName(),
                status,
                exception.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException exception) {
        log.error("Handling IllegalArgumentException: {}", exception.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = ErrorResponse.of(
                exception.getClass().getSimpleName(),
                status,
                exception.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {
        log.error("Handling DataIntegrityViolationException: {}", exception.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = ErrorResponse.of(
                "EventException",
                status,
                "A conflicting event is already scheduled at this venue on this date"
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception exception) {
        log.error("Handling unexpected Exception: {}", exception.getMessage(), exception);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse response = ErrorResponse.of(
                exception.getClass().getSimpleName(),
                status,
                exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred"
        );
        return ResponseEntity.status(status).body(response);
    }
}