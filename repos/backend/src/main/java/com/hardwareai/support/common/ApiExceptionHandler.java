package com.hardwareai.support.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> invalid(IllegalArgumentException e) {
        return body("INVALID_ARGUMENT", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> conflict(IllegalStateException e) {
        return body("INVALID_STATE", e.getMessage());
    }

    private Map<String, Object> body(String code, String message) {
        return Map.of("timestamp", Instant.now().toString(), "code", code, "message", message,
            "requestId", org.slf4j.MDC.get(RequestContextFilter.REQUEST_ID));
    }
}
