package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.exception.GlossaryException;
import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import com.aciworldwide.eccn_management_service.repository.GlossaryEntryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class GlossaryService {

    private final GlossaryEntryRepository glossaryEntryRepository;
    private final Set<String> validCategories = Set.of(
        "Export Controls",
        "Cryptography",
        "Hardware",
        "Software",
        "Technology",
        "General"
    );

    public GlossaryService(GlossaryEntryRepository glossaryEntryRepository) {
        this.glossaryEntryRepository = glossaryEntryRepository;
    }

    @Transactional
    @CacheEvict(value = "glossary", allEntries = true)
    public GlossaryEntry createEntry(GlossaryEntry entry) {
        validateEntry(entry);
        checkDuplicateTerm(entry.getTerm());
        entry.setLastUpdated(LocalDateTime.now().toString());
        try {
            return glossaryEntryRepository.save(entry);
        } catch (DuplicateKeyException e) {
            throw new GlossaryException(
                "Term already exists: " + entry.getTerm(),
                GlossaryException.ErrorCodes.DUPLICATE_TERM,
                e
            );
        }
    }

    @Transactional
    @CacheEvict(value = "glossary", allEntries = true)
    public GlossaryEntry updateEntry(String id, GlossaryEntry updatedEntry) {
        return glossaryEntryRepository.findById(id)
            .map(existingEntry -> {
                validateEntry(updatedEntry);
                // Allow the term to remain the same for the existing entry
                if (!existingEntry.getTerm().equals(updatedEntry.getTerm())) {
                    checkDuplicateTerm(updatedEntry.getTerm());
                }
                updatedEntry.setId(id);
                updatedEntry.setLastUpdated(LocalDateTime.now().toString());
                return glossaryEntryRepository.save(updatedEntry);
            })
            .orElseThrow(() -> new GlossaryException(
                "Glossary entry not found with id: " + id,
                GlossaryException.ErrorCodes.TERM_NOT_FOUND
            ));
    }

    public Optional<GlossaryEntry> findById(String id) {
        return glossaryEntryRepository.findById(id);
    }

    @Cacheable(value = "glossary", key = "#term")
    public GlossaryEntry getByTerm(String term) {
        return glossaryEntryRepository.findByTerm(term)
            .orElseThrow(() -> new GlossaryException(
                "Term not found: " + term,
                GlossaryException.ErrorCodes.TERM_NOT_FOUND
            ));
    }

    public List<GlossaryEntry> findByCategory(String category) {
        if (!validCategories.contains(category)) {
            throw new GlossaryException(
                "Invalid category: " + category,
                GlossaryException.ErrorCodes.INVALID_CATEGORY
            );
        }
        return glossaryEntryRepository.findByCategory(category);
    }

    public List<GlossaryEntry> searchByTermPart(String termPart) {
        if (termPart == null || termPart.trim().isEmpty()) {
            throw new GlossaryException(
                "Search term cannot be empty",
                GlossaryException.ErrorCodes.INVALID_TERM_FORMAT
            );
        }
        return glossaryEntryRepository.findByTermContainingIgnoreCase(termPart);
    }

    public List<GlossaryEntry> searchByDefinitionContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new GlossaryException(
                "Search text cannot be empty",
                GlossaryException.ErrorCodes.MISSING_REQUIRED_FIELD
            );
        }
        return glossaryEntryRepository.findByDefinitionContainingIgnoreCase(text);
    }

    public List<GlossaryEntry> findByRegulatoryContext(String context) {
        if (context == null || context.trim().isEmpty()) {
            throw new GlossaryException(
                "Regulatory context cannot be empty",
                GlossaryException.ErrorCodes.MISSING_REQUIRED_FIELD
            );
        }
        return glossaryEntryRepository.findByRegulatoryContextContainingIgnoreCase(context);
    }

    public List<GlossaryEntry> findByCrossReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            throw new GlossaryException(
                "Cross reference cannot be empty",
                GlossaryException.ErrorCodes.INVALID_CROSS_REFERENCE
            );
        }
        return glossaryEntryRepository.findByCrossReference(reference);
    }

    public List<GlossaryEntry> getAllEntries() {
        return glossaryEntryRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "glossary", allEntries = true)
    public void deleteEntry(String id) {
        if (!glossaryEntryRepository.existsById(id)) {
            throw new GlossaryException(
                "Cannot delete non-existent entry with id: " + id,
                GlossaryException.ErrorCodes.TERM_NOT_FOUND
            );
        }
        glossaryEntryRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "glossary", allEntries = true)
    public void importBulkEntries(List<GlossaryEntry> entries) {
        try {
            entries.forEach(this::validateEntry);
            entries.forEach(entry -> {
                checkDuplicateTerm(entry.getTerm());
                entry.setLastUpdated(LocalDateTime.now().toString());
            });
            glossaryEntryRepository.saveAll(entries);
        } catch (DuplicateKeyException e) {
            throw new GlossaryException(
                "Duplicate term encountered during bulk import: " + e.getMessage(),
                GlossaryException.ErrorCodes.DUPLICATE_TERM,
                e
            );
        } catch (Exception e) {
            throw new GlossaryException(
                "Error during bulk import: " + e.getMessage(),
                GlossaryException.ErrorCodes.BULK_IMPORT_ERROR,
                e
            );
        }
    }

    private void validateEntry(GlossaryEntry entry) {
        if (entry.getTerm() == null || entry.getTerm().trim().isEmpty()) {
            throw new GlossaryException(
                "Term cannot be null or empty",
                GlossaryException.ErrorCodes.MISSING_REQUIRED_FIELD
            );
        }
        if (entry.getDefinition() == null || entry.getDefinition().trim().isEmpty()) {
            throw new GlossaryException(
                "Definition cannot be null or empty",
                GlossaryException.ErrorCodes.MISSING_REQUIRED_FIELD
            );
        }
        if (entry.getCategory() != null && !validCategories.contains(entry.getCategory())) {
            throw new GlossaryException(
                "Invalid category: " + entry.getCategory(),
                GlossaryException.ErrorCodes.INVALID_CATEGORY
            );
        }
    }

    private void checkDuplicateTerm(String term) {
        glossaryEntryRepository.findByTerm(term).ifPresent(existing -> {
            throw new GlossaryException(
                "Term already exists: " + term,
                GlossaryException.ErrorCodes.DUPLICATE_TERM
            );
        });
    }

    public List<GlossaryEntry> findEntriesNeedingUpdate(String lastUpdateDate) {
        if (lastUpdateDate == null || lastUpdateDate.trim().isEmpty()) {
            throw new GlossaryException(
                "Last update date cannot be empty",
                GlossaryException.ErrorCodes.MISSING_REQUIRED_FIELD
            );
        }
        return glossaryEntryRepository.findAll().stream()
            .filter(entry -> entry.getLastUpdated().compareTo(lastUpdateDate) < 0)
            .toList();
    }
}