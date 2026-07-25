package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/glossary")
@RequiredArgsConstructor
public class GlossaryControllerV2 {

    private final GlossaryController v1Controller;

    @PostMapping
    public ResponseEntity<GlossaryEntry> createEntry(@Valid @RequestBody GlossaryEntry entry) {
        return v1Controller.createEntry(entry);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlossaryEntry> updateEntry(
            @PathVariable String id,
            @Valid @RequestBody GlossaryEntry entry) {
        return v1Controller.updateEntry(id, entry);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlossaryEntry> getEntryById(@PathVariable String id) {
        return v1Controller.getEntryById(id);
    }

    @GetMapping("/term/{term}")
    public ResponseEntity<GlossaryEntry> getEntryByTerm(@PathVariable String term) {
        return v1Controller.getEntryByTerm(term);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<GlossaryEntry>> getEntriesByCategory(@PathVariable String category) {
        return v1Controller.getEntriesByCategory(category);
    }

    @GetMapping("/search")
    public ResponseEntity<List<GlossaryEntry>> searchEntries(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String definition,
            @RequestParam(required = false) String context) {
        return v1Controller.searchEntries(term, definition, context);
    }

    @GetMapping("/cross-reference/{reference}")
    public ResponseEntity<List<GlossaryEntry>> getEntriesByCrossReference(
            @PathVariable String reference) {
        return v1Controller.getEntriesByCrossReference(reference);
    }

    @PostMapping("/bulk")
    public ResponseEntity<Void> importBulkEntries(@RequestBody List<@Valid GlossaryEntry> entries) {
        return v1Controller.importBulkEntries(entries);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable String id) {
        return v1Controller.deleteEntry(id);
    }

    @GetMapping("/updates")
    public ResponseEntity<List<GlossaryEntry>> getEntriesNeedingUpdate(
            @RequestParam String lastUpdateDate) {
        return v1Controller.getEntriesNeedingUpdate(lastUpdateDate);
    }
}
