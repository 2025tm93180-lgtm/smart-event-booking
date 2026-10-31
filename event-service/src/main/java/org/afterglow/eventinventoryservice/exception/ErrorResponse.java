package org.afterglow.eventinventoryservice.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String customException;
    private int httpStatusCode;
    private HttpStatus httpStatus;
    private String message;
    private LocalDateTime timestamp;

    public static ErrorResponse of(String customException, HttpStatus httpStatus, String message) {
        return ErrorResponse.builder()
                .customException(customException)
                .httpStatusCode(httpStatus != null ? httpStatus.value() : 500)
                .httpStatus(httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
