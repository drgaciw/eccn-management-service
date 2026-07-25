package com.aciworldwide.eccn_management_service.exception;

public class EccnValidationException extends EccnException {
    public EccnValidationException(String message) {
        super(message, "ECCN_VALIDATION", ErrorCategory.VALIDATION);
    }

    public EccnValidationException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.VALIDATION);
    }
}