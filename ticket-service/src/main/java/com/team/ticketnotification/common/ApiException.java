package com.team.ticketnotification.common;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public ApiException(HttpStatus status, String code, String message) {
        super(message); this.status=status; this.code=code;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","Resource not found."); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT,"STATE_CONFLICT",message); }
}
