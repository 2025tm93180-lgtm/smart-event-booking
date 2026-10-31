package com.team.ticketnotification.common;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    public static Map<String,Object> body(int status,String code,String message,String path) {
        return Map.of("timestamp",Instant.now().toString(),"status",status,"code",code,"message",message,"path",path,"correlationId",MDC.get("correlationId")==null ? "":MDC.get("correlationId"));
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException ex,HttpServletRequest request) { return ResponseEntity.status(ex.status()).body(body(ex.status().value(),ex.code(),ex.getMessage(),request.getRequestURI())); }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<?> invalid(Exception ex,HttpServletRequest request) { return ResponseEntity.badRequest().body(body(400,"INVALID_REQUEST","Invalid request parameter.",request.getRequestURI())); }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<?> missing(Exception ex,HttpServletRequest request) { return ResponseEntity.status(404).body(body(404,"NOT_FOUND","Resource not found.",request.getRequestURI())); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> forbidden(Exception ex,HttpServletRequest request) { return ResponseEntity.status(403).body(body(403,"FORBIDDEN","Access denied.",request.getRequestURI())); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception ex,HttpServletRequest request) {
        log.error("Request failed exceptionType={}",ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body(500,"INTERNAL_ERROR","Request could not be completed.",request.getRequestURI()));
    }
}
