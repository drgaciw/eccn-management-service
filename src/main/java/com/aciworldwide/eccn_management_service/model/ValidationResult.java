package com.aciworldwide.eccn_management_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of an ECCN validation operation.
 * Contains validation status, error messages, warnings, and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    /**
     * Indicates whether the validation was successful
     */
    private boolean valid;
    
    /**
     * List of validation error messages
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * List of validation warning messages
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * Additional information about the validation
     */
    private String message;
    
    /**
     * Timestamp when the validation was performed
     */
    private LocalDateTime validationTimestamp;
    
    /**
     * Source of the validation (e.g., "EXTERNAL_DATABASE", "LOCAL_RULES")
     */
    private String validationSource;
    
    /**
     * Confidence score of the validation (0.0 to 1.0)
     */
    private Double confidenceScore;
    
    /**
     * Additional metadata about the validation
     */
    private String metadata;
    
    /**
     * Creates a successful validation result
     * @return ValidationResult with valid=true
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .validationTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a successful validation result with a message
     * @param message Success message
     * @return ValidationResult with valid=true and message
     */
    public static ValidationResult success(String message) {
        return ValidationResult.builder()
                .valid(true)
                .message(message)
                .validationTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a failed validation result with an error message
     * @param error Error message
     * @return ValidationResult with valid=false and error
     */
    public static ValidationResult failure(String error) {
        ValidationResult result = ValidationResult.builder()
                .valid(false)
                .validationTimestamp(LocalDateTime.now())
                .build();
        result.getErrors().add(error);
        return result;
    }
    
    /**
     * Creates a failed validation result with multiple error messages
     * @param errors List of error messages
     * @return ValidationResult with valid=false and errors
     */
    public static ValidationResult failure(List<String> errors) {
        return ValidationResult.builder()
                .valid(false)
                .errors(new ArrayList<>(errors))
                .validationTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Adds an error message to the validation result
     * @param error Error message to add
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.valid = false;
    }
    
    /**
     * Adds a warning message to the validation result
     * @param warning Warning message to add
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
    
    /**
     * Checks if there are any errors
     * @return true if errors exist
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * Checks if there are any warnings
     * @return true if warnings exist
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}

