package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for linking two existing document records together.
 */
public record LinkDocumentsRequest(

    @NotNull
    @Schema(description = "ID of the first document record", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID documentId1,

    @NotNull
    @Schema(description = "ID of the second document record to link to the first", example = "9c858901-8a57-4791-81fe-4c455b099bc9")
    UUID documentId2,

    @NotBlank
    @Schema(description = "Type of relationship between the two documents", example = "SUPERSEDES")
    String relationshipType
) {}
