package com.hardwareai.support.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> invalid(IllegalArgumentException e) {
        log.warn("Handled client error [INVALID_ARGUMENT]: {}", e.getMessage());
        return body("INVALID_ARGUMENT", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> conflict(IllegalStateException e) {
        log.warn("Handled client error [INVALID_STATE]: {}", e.getMessage());
        return body("INVALID_STATE", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, Object> denied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return body("FORBIDDEN", "You are not allowed to perform this action");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Map<String, Object> unexpected(Exception e) {
        log.error("Unhandled server error", e);
        return body("INTERNAL_ERROR", "An unexpected error occurred");
    }

    private Map<String, Object> body(String code, String message) {
        return Map.of("timestamp", Instant.now().toString(), "code", code, "message", message,
                "requestId", org.slf4j.MDC.get(RequestContextFilter.REQUEST_ID));
    }
}
