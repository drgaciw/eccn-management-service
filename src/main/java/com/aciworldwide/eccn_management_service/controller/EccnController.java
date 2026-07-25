package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.dto.ClassificationSuggestionRequest;
import com.aciworldwide.eccn_management_service.dto.ClassificationSuggestionResponse;
import com.aciworldwide.eccn_management_service.dto.EccnValidationResult;
import com.aciworldwide.eccn_management_service.exception.InvalidEccnFormatException;
import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.service.AutomatedClassificationToolService;
import com.aciworldwide.eccn_management_service.service.AutomatedClassificationToolService.ModuleAnalysis;
import com.aciworldwide.eccn_management_service.service.EccnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/eccn")
@RequiredArgsConstructor
@Deprecated
@Tag(name = "ECCN Management (deprecated)", description = "Migrate to /api/eccn with X-API-Version: 1 header")
public class EccnController {

    private final EccnService eccnService;
    private final AutomatedClassificationToolService automatedClassificationToolService;

    @GetMapping
    @Operation(summary = "Get all ECCNs", description = "Retrieve all ECCN records, optionally filtered by exact category or by a control reason they contain")
    public ResponseEntity<List<Eccn>> getAllEccns(
            @Parameter(description = "Filter by exact category code (e.g. \"5\")") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by a control reason contained in the record's control reasons (e.g. \"NS\")") @RequestParam(required = false) String controlReason) {
        if (category == null && controlReason == null) {
            return ResponseEntity.ok(eccnService.findAllEccns());
        }
        Map<String, String> params = new HashMap<>();
        if (category != null) {
            params.put("category", category);
        }
        if (controlReason != null) {
            params.put("controlReason", controlReason);
        }
        return ResponseEntity.ok(eccnService.findAll(params));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ECCN by ID", description = "Retrieve a specific ECCN record by its ID")
    public ResponseEntity<Eccn> getEccnById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(eccnService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create ECCN record", description = "Create a new ECCN classification record")
    public ResponseEntity<Eccn> createEccn(@Valid @RequestBody Eccn eccn) {
        Eccn created = eccnService.createEccn(eccn);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/search")
    @Operation(summary = "Search ECCNs", description = "Search ECCN records by query string")
    public ResponseEntity<List<Eccn>> searchEccns(@RequestParam String query) {
        return ResponseEntity.ok(eccnService.searchEccns(query));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ECCN record", description = "Update an existing ECCN classification record")
    public ResponseEntity<Eccn> updateEccn(@PathVariable String id, @Valid @RequestBody Eccn eccn) {
        return ResponseEntity.ok(eccnService.updateEccn(id, eccn));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ECCN record", description = "Delete an existing ECCN classification record")
    public ResponseEntity<Void> deleteEccn(@PathVariable String id) {
        eccnService.deleteEccn(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/code/{eccnCode}")
    @Operation(summary = "Get ECCN by code",
        description = "Retrieve a single ECCN classification by its commodity code. Backed by "
            + "EccnService.findByCommodityCode(String); since commodityCode is uniquely indexed, "
            + "the first match is returned.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ECCN found"),
        @ApiResponse(responseCode = "400", description = "eccnCode does not match the required 5-character ECCN format"),
        @ApiResponse(responseCode = "404", description = "No ECCN record matches the given code")
    })
    public ResponseEntity<Eccn> getEccnByCode(@PathVariable String eccnCode) {
        try {
            return eccnService.findByCommodityCode(eccnCode).stream()
                    .findFirst()
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (InvalidEccnFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * NOTE: EccnService.getEccnHistory(String) is currently a stub — it always returns an
     * empty list (with an empty-list circuit-breaker fallback), regardless of the ECCN's
     * actual change history. This endpoint is wired to that existing method as-is; it will
     * return {@code []} for every ECCN until history tracking is implemented.
     */
    @GetMapping("/code/{eccnCode}/history")
    @Operation(summary = "Get ECCN history by code",
        description = "Retrieve change-history entries for the ECCN matching the given code. "
            + "KNOWN STUB: EccnService.getEccnHistory currently always returns an empty list, "
            + "so this endpoint returns [] until history tracking is implemented.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History entries for the ECCN (currently always empty — see description)"),
        @ApiResponse(responseCode = "400", description = "eccnCode does not match the required 5-character ECCN format"),
        @ApiResponse(responseCode = "404", description = "No ECCN record matches the given code")
    })
    public ResponseEntity<List<Eccn.EccnHistoryEntry>> getEccnHistoryByCode(@PathVariable String eccnCode) {
        try {
            return eccnService.findByCommodityCode(eccnCode).stream()
                    .findFirst()
                    .map(eccn -> ResponseEntity.ok(eccnService.getEccnHistory(eccn.getId())))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (InvalidEccnFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Returns a machine-generated classification SUGGESTION for human compliance review.
     * This endpoint never finalizes a classification or sets any CLASSIFIED/APPROVED state —
     * {@link ClassificationSuggestionResponse#reviewRequired()} is always {@code true}, and a
     * named human compliance approver must review and approve before any classification
     * becomes final. See PRD: all ECCN business outputs are decision support, not legal advice
     * or final export-control determinations.
     *
     * <p>KNOWN STUB: AutomatedClassificationToolService has no concrete AI model implementation
     * of its own content analysis. Its source/package analysis methods
     * (analyzeSourceCodeContent/analyzePackageContent) always report no encryption libraries
     * and no payment processing, so {@link AutomatedClassificationToolService#determineClassification}
     * will typically fall back to a generic code such as {@code EAR99}. This delegates directly
     * to that existing stub behaviour rather than adding new classification logic.</p>
     */
    @PostMapping("/classify")
    @Operation(summary = "Suggest an ECCN classification (human review required)",
        description = "Runs automated module analysis (AutomatedClassificationToolService) and returns a "
            + "SUGGESTED ECCN classification for human compliance review only. reviewRequired is always "
            + "true in the response; this endpoint never finalizes a classification or sets a "
            + "CLASSIFIED/APPROVED status. KNOWN LIMITATION: the underlying source/package content "
            + "analysis is a stub (no concrete AI model wiring); encryption-library and payment-processing "
            + "detection always return empty/false, so the suggested code will typically be a generic "
            + "fallback (e.g. EAR99) rather than a true content-derived classification.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suggestion generated (see description for stub limitations)"),
        @ApiResponse(responseCode = "400", description = "Request body failed validation")
    })
    public ResponseEntity<ClassificationSuggestionResponse> classify(@Valid @RequestBody ClassificationSuggestionRequest request) {
        // No packageType field exists on the request contract; the underlying service requires
        // a non-blank value, so a fixed placeholder is used. This does not affect the outcome
        // today because the analysis methods are stubs (see class Javadoc above).
        String packageType = "PRODUCT";
        ModuleAnalysis analysis = automatedClassificationToolService.analyzeSoftwarePackage(request.productName(), packageType);

        Map<String, Double> aiSuggestions = automatedClassificationToolService.suggestECCN(request.productName());
        double confidence = aiSuggestions.getOrDefault(analysis.getEccnClassification(), 0.0);
        List<ClassificationSuggestionResponse.AlternativeClassification> alternatives = aiSuggestions.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(analysis.getEccnClassification()))
                .map(entry -> new ClassificationSuggestionResponse.AlternativeClassification(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        ClassificationSuggestionResponse response = new ClassificationSuggestionResponse(
                analysis.getId(),
                analysis.getEccnClassification(),
                confidence,
                analysis.getClassificationRationale(),
                alternatives,
                true,
                analysis.getAnalysisTimestamp()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/{eccnCode}")
    @Operation(summary = "Validate ECCN code format",
        description = "Checks whether the given code matches the required 5-character ECCN format "
            + "(numbers and uppercase letters). This is a format check only — it does not confirm the "
            + "code is a registered classification, and it is not a compliance determination.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation result returned (valid=true, or valid=false with an explanatory message)")
    })
    public ResponseEntity<EccnValidationResult> validateEccnCode(@PathVariable String eccnCode) {
        try {
            eccnService.validateEccnFormat(eccnCode);
            return ResponseEntity.ok(EccnValidationResult.ofValid());
        } catch (InvalidEccnFormatException e) {
            return ResponseEntity.ok(EccnValidationResult.ofInvalid(e.getMessage()));
        }
    }

    /**
     * No service or repository method currently returns distinct ECCN categories, so this is
     * derived here from EccnService.findAllEccns() (thinnest delegation using existing data,
     * per the API-gap plan) rather than adding new business logic to the service layer.
     */
    @GetMapping("/categories")
    @Operation(summary = "Get distinct ECCN categories",
        description = "Derives the list of distinct category values currently present across all ECCN "
            + "records. No dedicated repository/service method exists for this; it is computed from "
            + "EccnService.findAllEccns() results.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distinct category values, sorted")
    })
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = eccnService.findAllEccns().stream()
                .map(Eccn::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }
}
