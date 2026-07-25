package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a new compliance document record.
 * Mirrors {@code DocumentRecordService#storeDocument} exactly; the service
 * has no parameter for a caller-supplied expiration date (it always derives
 * a 5-year default from the creation date), so no such field is offered here.
 */
public record CreateDocumentRecordRequest(

    @NotBlank
    @Schema(description = "Type of document, e.g. TECHNICAL_SPECS, CLASSIFICATION_JUSTIFICATION", example = "TECHNICAL_SPECS")
    String documentType,

    @NotBlank
    @Schema(description = "Human-readable name of the document", example = "Widget-3000 Technical Specification")
    String documentName,

    @Schema(description = "Free-text description of the document's contents", example = "Detailed technical specification supporting ECCN classification")
    String description,

    @NotBlank
    @Schema(description = "Path or reference to the actual stored document", example = "s3://compliance-docs/widget-3000/spec.pdf")
    String storageLocation,

    @NotBlank
    @Schema(description = "Software module or product this document is associated with", example = "Widget-3000")
    String associatedModule,

    @NotBlank
    @Schema(description = "ECCN classification this document supports", example = "5D002")
    String eccnClassification,

    @NotBlank
    @Schema(description = "Username or identifier of the record creator", example = "jdoe")
    String createdBy
) {}
