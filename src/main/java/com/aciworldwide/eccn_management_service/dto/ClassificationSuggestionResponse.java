package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response body for {@code POST /api/v1/eccn/classify}.
 *
 * <p><strong>This is a machine-generated SUGGESTION only.</strong> It is decision support,
 * not a legal or final export-control determination. A named human compliance approver
 * must review and approve before any classification becomes final, CLASSIFIED, or
 * otherwise authoritative. {@link #reviewRequired()} is always {@code true} for this
 * reason and this endpoint never sets an approved/classified status on any record.</p>
 *
 * <p><strong>Known stub limitation:</strong> the classification behind this suggestion is
 * produced by {@link com.aciworldwide.eccn_management_service.service.AutomatedClassificationToolService},
 * which currently has no concrete AI model implementation of its own content analysis —
 * its source/package analysis methods are stubs that always report no encryption
 * libraries and no payment processing. In practice {@link #eccnCode()} will typically
 * resolve to a generic fallback (e.g. {@code EAR99}) rather than a real, content-derived
 * classification, and {@link #confidence()} will be {@code 0.0} whenever no AI model
 * (Spring AI {@code ChatClient}) is configured.</p>
 */
public record ClassificationSuggestionResponse(
    @Schema(description = "Identifier of the persisted classification-history/analysis record backing this suggestion")
    String id,

    @Schema(description = "Suggested ECCN code. WARNING: source/package content analysis is currently a stub (see class Javadoc), so this typically resolves to a generic fallback such as EAR99 rather than a true content-derived classification.",
        requiredMode = Schema.RequiredMode.REQUIRED, example = "EAR99")
    String eccnCode,

    @Schema(description = "Confidence score between 0.0 and 1.0 from the optional AI suggestion model. 0.0 means either genuinely low confidence or — more likely today — that no AI model (ChatClient) is configured; treat 0.0 as 'unknown' rather than 'zero confidence'.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    double confidence,

    @Schema(description = "Human-readable rationale for the suggested classification, generated from the (stub) module analysis",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String reasoning,

    @Schema(description = "Other candidate ECCN codes with confidence scores, when the AI suggestion model returned more than one candidate. Empty when no AI model is configured.")
    List<AlternativeClassification> alternativeClassifications,

    @Schema(description = "Always true: this endpoint returns a SUGGESTION for human compliance review only. It never finalizes a classification or sets a CLASSIFIED/APPROVED status.",
        requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    boolean reviewRequired,

    @Schema(description = "Timestamp when this suggestion was generated", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime classifiedAt
) {
    public record AlternativeClassification(
        @Schema(description = "Candidate ECCN code", example = "5D992")
        String eccnCode,

        @Schema(description = "Confidence score for this candidate, between 0.0 and 1.0")
        double confidence
    ) {}
}
