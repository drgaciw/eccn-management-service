package com.aciworldwide.eccn_management_service.exception;

public class EccnValidationException extends EccnException {
    public EccnValidationException(String message) {
        super(message, "ECCN_VALIDATION", ErrorCategory.VALIDATION);
    }

    public EccnValidationException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.VALIDATION);
    }

    /**
     * For validation-shaped failures whose underlying cause is not itself a
     * {@code VALIDATION} problem (e.g. a duplicate-key conflict, which is a
     * {@code DATA_INTEGRITY} concern that should map to HTTP 409, not 400).
     */
    public EccnValidationException(String message, String errorCode, ErrorCategory category) {
        super(message, errorCode, category);
    }
}