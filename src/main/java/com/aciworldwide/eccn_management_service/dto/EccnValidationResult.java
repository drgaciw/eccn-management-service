package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of validating an ECCN code's format.
 * This is a lightweight format check only — it does not confirm the code is a real,
 * registered classification, and it is not a compliance determination.
 */
public record EccnValidationResult(
    @Schema(description = "True when the supplied code matches the required ECCN format (5 characters, numbers and uppercase letters)",
        requiredMode = Schema.RequiredMode.REQUIRED)
    boolean valid,

    @Schema(description = "Human-readable explanation, present when the code is invalid")
    String message
) {
    public static EccnValidationResult ofValid() {
        return new EccnValidationResult(true, null);
    }

    public static EccnValidationResult ofInvalid(String message) {
        return new EccnValidationResult(false, message);
    }
}
