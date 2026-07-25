package com.aciworldwide.eccn_management_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EccnException.class)
    public ResponseEntity<Map<String, Object>> handleEccnException(EccnException ex) {
        logger.error("ECCN exception occurred: {}", ex.getMessage(), ex);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", ex.getMessage());
        response.put("errorCode", ex.getErrorCode());
        response.put("category", ex.getCategory());

        HttpStatus status = determineHttpStatus(ex);
        response.put("status", status.value());

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("Validation failed: {}", details);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "Validation failed: " + details);
        response.put("errorCode", "VALIDATION_FAILED");
        response.put("status", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateKeyException(DuplicateKeyException ex) {
        logger.warn("Duplicate key exception: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "Duplicate record already exists");
        response.put("errorCode", "DUPLICATE_KEY");
        response.put("status", HttpStatus.CONFLICT.value());

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "Access is denied");
        response.put("errorCode", "ACCESS_DENIED");
        response.put("status", HttpStatus.FORBIDDEN.value());

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", ex.getMessage());
        response.put("errorCode", "INVALID_ARGUMENT");
        response.put("status", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * {@link InvalidEccnFormatException} extends bare {@code RuntimeException} rather than
     * {@link EccnException}, so without this handler it falls through to
     * {@link #handleGenericException} and is reported as an opaque 500 instead of the
     * validation failure it actually is. GitNexus impact analysis on
     * {@code InvalidEccnFormatException} showed HIGH risk (11 impacted symbols across
     * createEccn/updateEccn/bulkCreateEccn/findByCommodityCode), so this handler is added
     * here instead of changing that class's type hierarchy.
     */
    @ExceptionHandler(InvalidEccnFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEccnFormatException(InvalidEccnFormatException ex) {
        logger.warn("Invalid ECCN format: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", ex.getMessage());
        response.put("errorCode", "INVALID_FORMAT");
        response.put("status", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private HttpStatus determineHttpStatus(EccnException ex) {
        return switch (ex.getCategory()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case SECURITY -> HttpStatus.FORBIDDEN;
            case DATA_INTEGRITY -> HttpStatus.CONFLICT;
            case TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the full exception for debugging purposes
        logger.error("Unhandled exception occurred", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "An unexpected error occurred");
        response.put("errorCode", "INTERNAL_ERROR");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
