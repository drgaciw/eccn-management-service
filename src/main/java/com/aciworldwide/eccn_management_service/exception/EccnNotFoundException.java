package com.aciworldwide.eccn_management_service.exception;

public class EccnNotFoundException extends EccnException {
    public EccnNotFoundException(String message) {
        super(message, ErrorCodes.NOT_FOUND, ErrorCategory.VALIDATION);
    }
}