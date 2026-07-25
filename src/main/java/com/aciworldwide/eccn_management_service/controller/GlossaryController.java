package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.exception.GlossaryException;
import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import com.aciworldwide.eccn_management_service.service.GlossaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/glossary")
@RequiredArgsConstructor
@Deprecated
@Tag(name = "Glossary (deprecated)", description = "Migrate to /api/glossary with X-API-Version: 1 header")
public class GlossaryController {

    private final GlossaryService glossaryService;

    @PostMapping
    public ResponseEntity<GlossaryEntry> createEntry(@Valid @RequestBody GlossaryEntry entry) {
        return new ResponseEntity<>(glossaryService.createEntry(entry), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlossaryEntry> updateEntry(
            @PathVariable String id,
            @Valid @RequestBody GlossaryEntry entry) {
        return ResponseEntity.ok(glossaryService.updateEntry(id, entry));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlossaryEntry> getEntryById(@PathVariable String id) {
        return glossaryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/term/{term}")
    public ResponseEntity<GlossaryEntry> getEntryByTerm(@PathVariable String term) {
        try {
            return ResponseEntity.ok(glossaryService.getByTerm(term));
        } catch (GlossaryException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<GlossaryEntry>> getEntriesByCategory(@PathVariable String category) {
        List<GlossaryEntry> entries = glossaryService.findByCategory(category);
        return entries.isEmpty() 
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(entries);
    }

    @GetMapping("/search")
    public ResponseEntity<List<GlossaryEntry>> searchEntries(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String definition,
            @RequestParam(required = false) String context) {
        
        List<GlossaryEntry> results;
        
        if (term != null) {
            results = glossaryService.searchByTermPart(term);
        } else if (definition != null) {
            results = glossaryService.searchByDefinitionContent(definition);
        } else if (context != null) {
            results = glossaryService.findByRegulatoryContext(context);
        } else {
            results = glossaryService.getAllEntries();
        }

        return results.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(results);
    }

    @GetMapping("/cross-reference/{reference}")
    public ResponseEntity<List<GlossaryEntry>> getEntriesByCrossReference(
            @PathVariable String reference) {
        List<GlossaryEntry> entries = glossaryService.findByCrossReference(reference);
        return entries.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(entries);
    }

    @PostMapping("/bulk")
    public ResponseEntity<Void> importBulkEntries(@RequestBody List<@Valid GlossaryEntry> entries) {
        glossaryService.importBulkEntries(entries);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable String id) {
        glossaryService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/updates")
    public ResponseEntity<List<GlossaryEntry>> getEntriesNeedingUpdate(
            @RequestParam String lastUpdateDate) {
        List<GlossaryEntry> entries = glossaryService.findEntriesNeedingUpdate(lastUpdateDate);
        return entries.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(entries);
    }
}