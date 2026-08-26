package org.civicfix.app.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public ApiError(HttpStatus status, String message) {
        this(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, null);
    }

    public ApiError(HttpStatus status, String message, Map<String, String> fieldErrors) {
        this(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
