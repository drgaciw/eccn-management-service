package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for adding a new version of a document's content.
 */
public record CreateDocumentVersionRequest(

    @NotBlank
    @Schema(description = "Full content of the new document version", example = "Revised technical specification text...")
    String content
) {}
