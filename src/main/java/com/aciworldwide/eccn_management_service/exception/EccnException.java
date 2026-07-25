package com.aciworldwide.eccn_management_service.exception;

import lombok.Getter;

@Getter
public class EccnException extends RuntimeException {
    private final String errorCode;
    private final ErrorCategory category;

    public EccnException(String message, String errorCode, ErrorCategory category) {
        super(message);
        this.errorCode = errorCode;
        this.category = category;
    }

    public EccnException(String message, String errorCode, ErrorCategory category, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.category = category;
    }

    public enum ErrorCategory {
        VALIDATION,
        BUSINESS_RULE,
        TECHNICAL,
        SECURITY,
        DATA_INTEGRITY
    }

    public static class ErrorCodes {
        public static final String INVALID_FORMAT = "ECCN_001";
        public static final String INVALID_CATEGORY = "ECCN_002";
        public static final String INVALID_SUBCATEGORY = "ECCN_003";
        public static final String INVALID_CONTROL_REASON = "ECCN_004";
        public static final String INVALID_DESCRIPTION = "ECCN_005";
        public static final String DUPLICATE_CODE = "ECCN_006";
        public static final String NOT_FOUND = "ECCN_007";
        public static final String ACCESS_DENIED = "ECCN_008";
        public static final String DATABASE_ERROR = "ECCN_009";
        public static final String INVALID_STATE = "ECCN_010";
    }
}