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
        return Map.of("timestamp", Instant.now().toString(), "message", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> conflict(IllegalStateException e) {
        return Map.of("timestamp", Instant.now().toString(), "message", e.getMessage());
    }
}
