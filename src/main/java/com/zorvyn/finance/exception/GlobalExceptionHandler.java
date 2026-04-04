package com.zorvyn.finance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return buildError(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // 403 - Custom access denied (Service layer)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    // 403 - Spring Security (@PreAuthorize)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return buildError("Access Denied", HttpStatus.FORBIDDEN);
    }

    // 401 - Missing/invalid authentication
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedSpring(
            AuthenticationCredentialsNotFoundException ex) {
        return buildError("Unauthorized", HttpStatus.UNAUTHORIZED);
    }

    // 401 - Custom unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        return buildError(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("details", fieldErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 400 - Bad request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return buildError("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private ResponseEntity<Map<String, String>> buildError(String message, HttpStatus status) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return new ResponseEntity<>(body, status);
    }

    // 400 - Malformed JSON body
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid JSON format");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}