package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.service.EccnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/eccn")
@RequiredArgsConstructor
@Deprecated
@Tag(name = "ECCN Management (deprecated)", description = "Migrate to /api/eccn with X-API-Version: 1 header")
public class EccnController {

    private final EccnService eccnService;

    @GetMapping
    @Operation(summary = "Get all ECCNs", description = "Retrieve all ECCN records with optional filtering")
    public ResponseEntity<List<Eccn>> getAllEccns(@RequestParam(required = false) Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return ResponseEntity.ok(eccnService.findAllEccns());
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
}
