package com.aciworldwide.eccn_management_service.exception;

public class IntegrationException extends Exception {
    public IntegrationException(String message) {
        super(message);
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}