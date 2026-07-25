package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.model.Eccn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eccn")
@RequiredArgsConstructor
@Tag(name = "ECCN Management (v1)", description = "ECCN Management API — header-based versioning via X-API-Version: 1")
public class EccnControllerV2 {

    private final EccnController v1Controller;

    @GetMapping
    @Operation(summary = "Get all ECCNs", description = "Retrieve all ECCN records with optional filtering")
    public ResponseEntity<List<Eccn>> getAllEccns(@RequestParam(required = false) Map<String, String> params) {
        return v1Controller.getAllEccns(params);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ECCN by ID", description = "Retrieve a specific ECCN record by its ID")
    public ResponseEntity<Eccn> getEccnById(@PathVariable String id) {
        return v1Controller.getEccnById(id);
    }

    @PostMapping
    @Operation(summary = "Create ECCN record", description = "Create a new ECCN classification record")
    public ResponseEntity<Eccn> createEccn(@Valid @RequestBody Eccn eccn) {
        return v1Controller.createEccn(eccn);
    }

    @GetMapping("/search")
    @Operation(summary = "Search ECCNs", description = "Search ECCN records by query string")
    public ResponseEntity<List<Eccn>> searchEccns(@RequestParam String query) {
        return v1Controller.searchEccns(query);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ECCN record", description = "Update an existing ECCN classification record")
    public ResponseEntity<Eccn> updateEccn(@PathVariable String id, @Valid @RequestBody Eccn eccn) {
        return v1Controller.updateEccn(id, eccn);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ECCN record", description = "Delete an existing ECCN classification record")
    public ResponseEntity<Void> deleteEccn(@PathVariable String id) {
        return v1Controller.deleteEccn(id);
    }
}
