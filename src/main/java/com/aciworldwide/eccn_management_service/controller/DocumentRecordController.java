package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.dto.CreateDocumentRecordRequest;
import com.aciworldwide.eccn_management_service.dto.CreateDocumentVersionRequest;
import com.aciworldwide.eccn_management_service.dto.GenerateDocumentRequest;
import com.aciworldwide.eccn_management_service.dto.LinkDocumentsRequest;
import com.aciworldwide.eccn_management_service.model.DocumentRecord;
import com.aciworldwide.eccn_management_service.model.DocumentVersion;
import com.aciworldwide.eccn_management_service.service.DocumentRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Thin REST facade over {@link DocumentRecordService} (PRD Story 5 — Manage
 * Compliance Documentation). Every endpoint here delegates directly to an
 * existing public service method; no new business logic is introduced.
 *
 * <p>Note on IDs: {@code documentId} path variables are declared as
 * {@code String} and converted with {@link UUID#fromString(String)} so that
 * a malformed ID surfaces as an {@link IllegalArgumentException}, which
 * {@code GlobalExceptionHandler} already maps to {@code 400 BAD_REQUEST} —
 * the same status the service throws for "document not found".
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Compliance Documentation", description = "Manage compliance document records, versions, links, and retention (PRD Story 5)")
public class DocumentRecordController {

    private final DocumentRecordService documentRecordService;

    @PostMapping
    @Operation(summary = "Create document record",
            description = "Creates a new compliance document record with type, name, module, ECCN classification, storage location, and creator. "
                    + "Expiration date is not a caller-supplied field: the service always derives a fixed 5-year retention window from the creation date.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document record created"),
            @ApiResponse(responseCode = "400", description = "Request body failed validation"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<DocumentRecord> createDocument(@Valid @RequestBody CreateDocumentRecordRequest request) {
        DocumentRecord created = documentRecordService.storeDocument(
                request.documentType(),
                request.documentName(),
                request.description(),
                request.storageLocation(),
                request.associatedModule(),
                request.eccnClassification(),
                request.createdBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/generated")
    @Operation(summary = "Generate document from template",
            description = "Generates a placeholder document record from a template name. "
                    + "The underlying service implementation currently ignores the supplied data map and always stores a fixed 'Generated' document type with ECCN 'N/A'.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Generated document record created"),
            @ApiResponse(responseCode = "400", description = "Request body failed validation"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<DocumentRecord> generateDocument(@Valid @RequestBody GenerateDocumentRequest request) {
        DocumentRecord created = documentRecordService.generateDocument(request.templateName(), request.data());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{documentId}/versions")
    @Operation(summary = "Create document version",
            description = "Adds a new content version to an existing document record.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document version created"),
            @ApiResponse(responseCode = "400", description = "Invalid document ID, document not found, or request body failed validation"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<DocumentVersion> createDocumentVersion(
            @Parameter(description = "ID of the document record to add a version to", required = true)
            @PathVariable String documentId,
            @Valid @RequestBody CreateDocumentVersionRequest request) {
        DocumentVersion version = documentRecordService.createDocumentVersion(
                UUID.fromString(documentId), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    @GetMapping("/{documentId}/versions")
    @Operation(summary = "List document versions",
            description = "Lists all versions of a document, ordered by version number descending (newest first).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versions retrieved (may be empty)"),
            @ApiResponse(responseCode = "400", description = "Invalid document ID format"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<DocumentVersion>> getDocumentVersions(
            @Parameter(description = "ID of the document record", required = true)
            @PathVariable String documentId) {
        List<DocumentVersion> versions = documentRecordService.getDocumentVersions(UUID.fromString(documentId));
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{documentId}/versions/compare")
    @Operation(summary = "Compare two document versions",
            description = "Returns a line-by-line diff between two versions of the same document.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diff generated"),
            @ApiResponse(responseCode = "400", description = "Invalid document ID, or either version number not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<String> compareDocumentVersions(
            @Parameter(description = "ID of the document record", required = true)
            @PathVariable String documentId,
            @Parameter(description = "First version number to compare", required = true, example = "1")
            @RequestParam int version1,
            @Parameter(description = "Second version number to compare", required = true, example = "2")
            @RequestParam int version2) {
        String diff = documentRecordService.compareDocumentVersions(UUID.fromString(documentId), version1, version2);
        return ResponseEntity.ok(diff);
    }

    @PostMapping("/links")
    @Operation(summary = "Link related documents",
            description = "Links two existing document records together under an explicit relationship type (e.g. SUPERSEDES, SUPPORTS).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Documents linked"),
            @ApiResponse(responseCode = "400", description = "Either document not found, or request body failed validation"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<Void> linkDocuments(@Valid @RequestBody LinkDocumentsRequest request) {
        documentRecordService.linkDocuments(request.documentId1(), request.documentId2(), request.relationshipType());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-type/{documentType}")
    @Operation(summary = "Find documents by type",
            description = "Lists document records matching the given document type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved (may be empty)"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<DocumentRecord>> getDocumentsByType(
            @Parameter(description = "Document type, e.g. TECHNICAL_SPECS", required = true)
            @PathVariable String documentType) {
        return ResponseEntity.ok(documentRecordService.getDocumentsByType(documentType));
    }

    @GetMapping("/by-module/{moduleName}")
    @Operation(summary = "Find documents by associated module",
            description = "Lists document records associated with the given software module.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved (may be empty)"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<DocumentRecord>> getDocumentsByModule(
            @Parameter(description = "Associated software module name", required = true)
            @PathVariable String moduleName) {
        return ResponseEntity.ok(documentRecordService.getDocumentsByModule(moduleName));
    }

    @GetMapping("/by-eccn/{eccnClassification}")
    @Operation(summary = "Find documents by ECCN classification",
            description = "Lists document records associated with the given ECCN classification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved (may be empty)"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<DocumentRecord>> getDocumentsByEccn(
            @Parameter(description = "ECCN classification, e.g. 5D002", required = true)
            @PathVariable String eccnClassification) {
        return ResponseEntity.ok(documentRecordService.getDocumentsByEccn(eccnClassification));
    }

    @GetMapping("/by-type-and-module")
    @Operation(summary = "Find documents by type and module",
            description = "Lists document records matching both the given document type and associated module.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved (may be empty)"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<DocumentRecord>> getDocumentsByTypeAndModule(
            @Parameter(description = "Document type, e.g. TECHNICAL_SPECS", required = true)
            @RequestParam String documentType,
            @Parameter(description = "Associated software module name", required = true)
            @RequestParam String moduleName) {
        return ResponseEntity.ok(documentRecordService.getDocumentsByTypeAndModule(documentType, moduleName));
    }

    @GetMapping("/search")
    @Operation(summary = "Search documents by name",
            description = "Case-insensitive substring search over document names.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved (may be empty)"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<DocumentRecord>> searchDocuments(
            @Parameter(description = "Substring to search for within document names", required = true)
            @RequestParam String searchTerm) {
        return ResponseEntity.ok(documentRecordService.searchDocuments(searchTerm));
    }

    @GetMapping("/audit-trail/{username}")
    @Operation(summary = "Get audit trail by creator",
            description = "Lists document records created by the given username, for audit purposes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved (may be empty)"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<DocumentRecord>> getAuditTrail(
            @Parameter(description = "Username or identifier of the record creator", required = true)
            @PathVariable String username) {
        return ResponseEntity.ok(documentRecordService.getAuditTrail(username));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Find documents by creation date range",
            description = "Lists document records created between the given start and end dates (inclusive).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved (may be empty)"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error, including malformed date parameters")
    })
    public ResponseEntity<List<DocumentRecord>> getDocumentsByDateRange(
            @Parameter(description = "Start of the creation date range (ISO-8601, e.g. 2026-01-01)", required = true, example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End of the creation date range (ISO-8601, e.g. 2026-12-31)", required = true, example = "2026-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(documentRecordService.getDocumentsByDateRange(startDate, endDate));
    }

    @PostMapping("/archive-expired")
    @Operation(summary = "Archive expired documents",
            description = "Explicitly triggers archival of all document records whose expiration date has passed. "
                    + "This is a deliberate, human-initiated action — it is never triggered automatically as a side effect of another operation.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Expired documents archived"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<Void> archiveExpiredDocuments() {
        documentRecordService.archiveExpiredDocuments();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/archived")
    @Operation(summary = "Delete archived documents",
            description = "Explicitly triggers permanent deletion of all document records currently marked archived, per retention policy. "
                    + "This is a deliberate, human-initiated action — it is never triggered automatically as a side effect of another operation. "
                    + "The underlying service does not currently accept scoping parameters (e.g. a specific document ID or date cutoff); it deletes every archived record in one call.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Archived documents deleted"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<Void> deleteArchivedDocuments() {
        documentRecordService.deleteArchivedDocuments();
        return ResponseEntity.noContent().build();
    }
}
