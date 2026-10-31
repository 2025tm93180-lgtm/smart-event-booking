package org.afterglow.eventinventoryservice.exception;

import org.springframework.http.HttpStatus;

public class VenueException extends RuntimeException {

    private final HttpStatus httpStatus;

    public VenueException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public VenueException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public VenueException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public VenueException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
