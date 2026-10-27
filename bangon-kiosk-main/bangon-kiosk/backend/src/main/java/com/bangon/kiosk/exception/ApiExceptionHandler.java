package com.bangon.kiosk.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

/** Converts exceptions into JSON error responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class,
            MethodArgumentNotValidException.class})
    ResponseEntity<?> badRequest(Exception e) {
        String message = e instanceof MethodArgumentNotValidException
                ? "Invalid request fields"
                : e.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<?> credentials(Exception e) {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> denied(Exception e) {
        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
    }
}
