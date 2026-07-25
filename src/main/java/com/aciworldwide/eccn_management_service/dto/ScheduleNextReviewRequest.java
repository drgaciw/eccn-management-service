package com.aciworldwide.eccn_management_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Request to reschedule the next review date of a risk assessment")
public record ScheduleNextReviewRequest(

    @Min(1)
    @Max(60)
    @Schema(description = "Number of months from today until the next review is due", example = "6",
            requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "60")
    int months
) {}
