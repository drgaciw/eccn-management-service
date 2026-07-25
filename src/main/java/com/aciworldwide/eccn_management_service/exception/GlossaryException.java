package com.aciworldwide.eccn_management_service.exception;

public class GlossaryException extends RuntimeException {
    
    private final String errorCode;

    public GlossaryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public GlossaryException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class ErrorCodes {
        public static final String TERM_NOT_FOUND = "GLOSSARY_001";
        public static final String DUPLICATE_TERM = "GLOSSARY_002";
        public static final String INVALID_TERM_FORMAT = "GLOSSARY_003";
        public static final String MISSING_REQUIRED_FIELD = "GLOSSARY_004";
        public static final String INVALID_CATEGORY = "GLOSSARY_005";
        public static final String BULK_IMPORT_ERROR = "GLOSSARY_006";
        public static final String INVALID_CROSS_REFERENCE = "GLOSSARY_007";
    }
}