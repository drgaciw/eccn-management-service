package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request to create a new export-control risk assessment for a module")
public record CreateRiskAssessmentRequest(

    @NotBlank
    @Schema(description = "Name of the module being assessed", example = "payment-gateway-core",
            requiredMode = Schema.RequiredMode.REQUIRED)
    String moduleName,

    @NotNull
    @Schema(description = "Restricted end uses identified for this module (each contributes 10 points to the risk score)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> restrictedEndUses,

    @NotNull
    @Schema(description = "High-risk users identified for this module (each contributes 5 points to the risk score)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> highRiskUsers,

    @NotNull
    @Schema(description = "Third-party components identified for this module (each contributes 3 points to the risk score)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> thirdPartyComponents,

    @NotBlank
    @Schema(description = "Named human assessor performing this risk assessment", example = "jane.compliance",
            requiredMode = Schema.RequiredMode.REQUIRED)
    String assessedBy
) {}
