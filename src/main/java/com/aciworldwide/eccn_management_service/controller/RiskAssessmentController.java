package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.dto.CreateRiskAssessmentRequest;
import com.aciworldwide.eccn_management_service.dto.ScheduleNextReviewRequest;
import com.aciworldwide.eccn_management_service.dto.UpdateMitigationActionsRequest;
import com.aciworldwide.eccn_management_service.model.RiskAssessment;
import com.aciworldwide.eccn_management_service.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes {@link RiskAssessmentService} over REST.
 *
 * <p>This controller is a thin wrapper: it does not implement any scoring, threshold, or
 * persistence logic of its own. Every endpoint delegates directly to an existing public method
 * on {@link RiskAssessmentService}. Search criteria and behaviors that the service does not
 * currently implement (search by high-risk user, by assessor, or by follow-up flag; single-record
 * retrieval by id) are intentionally not exposed here — see the project README/PR notes for the
 * full gap list rather than reimplementing them against the repository directly.</p>
 */
@RestController
@RequestMapping("/api/v1/risk-assessments")
@RequiredArgsConstructor
@Tag(name = "Risk Assessments", description = "Export-control risk assessment scoring, search, and mitigation tracking")
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    @PostMapping
    @Operation(summary = "Create a risk assessment",
            description = "Scores a module for export-control risk from restricted end uses (10 pts each), " +
                    "high-risk users (5 pts each), and third-party components (3 pts each). Risk level is " +
                    "LOW (score <= 10), MEDIUM (score <= 30), or HIGH (score > 30). assessmentDate defaults " +
                    "to today and nextReviewDate defaults to six months from today.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Risk assessment created",
                    content = @Content(schema = @Schema(implementation = RiskAssessment.class))),
            @ApiResponse(responseCode = "400", description = "Request failed validation"),
            @ApiResponse(responseCode = "409", description = "A risk assessment for this module already exists")
    })
    public ResponseEntity<RiskAssessment> createRiskAssessment(@Valid @RequestBody CreateRiskAssessmentRequest request) {
        RiskAssessment created = riskAssessmentService.createRiskAssessment(
                request.moduleName(),
                request.restrictedEndUses(),
                request.highRiskUsers(),
                request.thirdPartyComponents(),
                request.assessedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/search")
    @Operation(summary = "Search risk assessments",
            description = "Filters by exactly one criterion at a time, in this precedence order when more than " +
                    "one is supplied: moduleName, riskLevel, endUse, thirdPartyComponent. Searching by high-risk " +
                    "user, by assessor, or by follow-up flag is not currently supported — the underlying service " +
                    "does not expose those lookups.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching risk assessments returned (possibly empty)",
                    content = @Content(schema = @Schema(implementation = RiskAssessment.class))),
            @ApiResponse(responseCode = "400", description = "No search criterion was provided")
    })
    public ResponseEntity<List<RiskAssessment>> searchRiskAssessments(
            @Parameter(description = "Exact module name to search for") @RequestParam(required = false) String moduleName,
            @Parameter(description = "Risk level to search for (LOW, MEDIUM, or HIGH)") @RequestParam(required = false) String riskLevel,
            @Parameter(description = "Restricted end use substring to search for") @RequestParam(required = false) String endUse,
            @Parameter(description = "Third-party component substring to search for") @RequestParam(required = false) String thirdPartyComponent) {

        List<RiskAssessment> results;
        if (moduleName != null) {
            results = riskAssessmentService.getAssessmentsByModule(moduleName);
        } else if (riskLevel != null) {
            results = riskAssessmentService.getAssessmentsByRiskLevel(riskLevel);
        } else if (endUse != null) {
            results = riskAssessmentService.searchAssessmentsByEndUse(endUse);
        } else if (thirdPartyComponent != null) {
            results = riskAssessmentService.searchAssessmentsByThirdPartyComponent(thirdPartyComponent);
        } else {
            throw new IllegalArgumentException(
                    "At least one search criterion (moduleName, riskLevel, endUse, thirdPartyComponent) is required");
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/high-risk")
    @Operation(summary = "Get all HIGH risk assessments",
            description = "Returns every assessment currently scored at HIGH risk level (score > 30).")
    @ApiResponse(responseCode = "200", description = "HIGH risk assessments returned (possibly empty)",
            content = @Content(schema = @Schema(implementation = RiskAssessment.class)))
    public ResponseEntity<List<RiskAssessment>> getHighRiskAssessments() {
        return ResponseEntity.ok(riskAssessmentService.getHighRiskAssessments());
    }

    @GetMapping("/due-for-review")
    @Operation(summary = "Get assessments due for review",
            description = "Returns assessments whose nextReviewDate is before today. The underlying service does " +
                    "not accept an arbitrary as-of date, so only \"due as of now\" can be queried.")
    @ApiResponse(responseCode = "200", description = "Assessments due for review returned (possibly empty)",
            content = @Content(schema = @Schema(implementation = RiskAssessment.class)))
    public ResponseEntity<List<RiskAssessment>> getAssessmentsDueForReview() {
        return ResponseEntity.ok(riskAssessmentService.getAssessmentsRequiringReview());
    }

    @PatchMapping("/{id}/follow-up")
    @Operation(summary = "Flag a risk assessment for follow-up",
            description = "Sets requiresFollowUp to true. This only raises a follow-up flag; it never closes, " +
                    "waives, or auto-resolves a finding. If no assessment exists with the given id, the " +
                    "underlying service silently takes no action and this endpoint still returns 204.")
    @ApiResponse(responseCode = "204", description = "Follow-up flag set (or id did not match any assessment)")
    public ResponseEntity<Void> flagForFollowUp(@PathVariable String id) {
        riskAssessmentService.flagForFollowUp(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/mitigation-actions")
    @Operation(summary = "Update mitigation actions",
            description = "Replaces the full list of mitigation actions recorded for the assessment. " +
                    "The RiskAssessment model has no field to record who made a mitigation change, so this " +
                    "endpoint cannot attribute the update to a named actor beyond what request logging captures. " +
                    "If no assessment exists with the given id, the underlying service silently takes no action " +
                    "and this endpoint still returns 204.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mitigation actions updated (or id did not match any assessment)"),
            @ApiResponse(responseCode = "400", description = "Request failed validation")
    })
    public ResponseEntity<Void> updateMitigationActions(@PathVariable String id,
                                                          @Valid @RequestBody UpdateMitigationActionsRequest request) {
        riskAssessmentService.updateMitigationActions(id, request.mitigationActions());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/next-review")
    @Operation(summary = "Schedule the next review date",
            description = "Sets nextReviewDate to today plus the given number of months. If no assessment " +
                    "exists with the given id, the underlying service silently takes no action and this " +
                    "endpoint still returns 204.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Next review date scheduled (or id did not match any assessment)"),
            @ApiResponse(responseCode = "400", description = "Request failed validation")
    })
    public ResponseEntity<Void> scheduleNextReview(@PathVariable String id,
                                                    @Valid @RequestBody ScheduleNextReviewRequest request) {
        riskAssessmentService.scheduleNextReview(id, request.months());
        return ResponseEntity.noContent().build();
    }
}
