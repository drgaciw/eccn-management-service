package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request to replace the mitigation actions recorded against a risk assessment")
public record UpdateMitigationActionsRequest(

    @NotNull
    @Schema(description = "Full list of mitigation actions to record for this assessment " +
            "(replaces any existing list)", requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> mitigationActions
) {}
