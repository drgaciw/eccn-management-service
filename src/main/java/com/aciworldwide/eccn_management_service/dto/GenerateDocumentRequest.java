package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Request payload for generating a document from a template.
 * Mirrors {@code DocumentRecordService#generateDocument}; note that the
 * underlying service currently ignores the {@code data} map entirely and
 * always stores a fixed placeholder record (see controller-level Javadoc).
 */
public record GenerateDocumentRequest(

    @NotBlank
    @Schema(description = "Name of the template to generate the document from", example = "classification-justification-template")
    String templateName,

    @Schema(description = "Template data values (currently unused by the underlying service implementation)")
    Map<String, String> data
) {}
