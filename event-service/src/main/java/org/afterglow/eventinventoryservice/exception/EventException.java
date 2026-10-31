package org.afterglow.eventinventoryservice.exception;

import org.springframework.http.HttpStatus;

public class EventException extends RuntimeException {

    private final HttpStatus httpStatus;

    public EventException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public EventException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public EventException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public EventException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
