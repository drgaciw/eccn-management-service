package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Request body for {@code POST /api/v1/eccn/classify}.
 *
 * <p><strong>Known limitation:</strong> {@link com.aciworldwide.eccn_management_service.service.AutomatedClassificationToolService}
 * has no concrete AI model implementation of its own content analysis — its source/package
 * analysis methods are stubs that always report no encryption libraries and no payment
 * processing, regardless of what is submitted here. {@code description} and
 * {@code technicalSpecs} are accepted for audit/traceability purposes and forward
 * compatibility but are NOT currently analyzed. See {@link ClassificationSuggestionResponse}
 * for how the resulting stub behaviour is surfaced to callers.</p>
 */
public record ClassificationSuggestionRequest(
    @NotBlank
    @Schema(description = "Identifier of the product being classified", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROD-1234")
    String productId,

    @NotBlank
    @Schema(description = "Name of the product/module/package being classified; used as the module name for automated analysis and classification history",
        requiredMode = Schema.RequiredMode.REQUIRED, example = "PaymentGatewaySDK")
    String productName,

    @NotBlank
    @Schema(description = "Description of the product. NOTE: not currently analyzed — the underlying analysis methods are stubs that ignore this field.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String description,

    @Schema(description = "Optional free-form technical specifications. NOTE: not currently analyzed — accepted for forward compatibility only.")
    Map<String, Object> technicalSpecs,

    @NotBlank
    @Schema(description = "Person or system requesting the classification, retained for audit purposes", requiredMode = Schema.RequiredMode.REQUIRED, example = "jane.doe@example.com")
    String requestedBy,

    @Schema(description = "Requested priority of this classification request", example = "Medium", allowableValues = {"High", "Medium", "Low"})
    String priority
) {}
