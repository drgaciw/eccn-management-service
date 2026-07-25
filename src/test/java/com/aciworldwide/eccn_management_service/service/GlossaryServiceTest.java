package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.exception.GlossaryException;
import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import com.aciworldwide.eccn_management_service.repository.GlossaryEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GlossaryServiceTest {

    @Mock
    private GlossaryEntryRepository glossaryEntryRepository;

    @InjectMocks
    private GlossaryService glossaryService;

    private GlossaryEntry testEntry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testEntry = new GlossaryEntry();
        testEntry.setId("1");
        testEntry.setTerm("ECCN");
        testEntry.setDefinition("Export Control Classification Number");
        testEntry.setRegulatoryContext("BIS Export Administration Regulations");
        testEntry.setCategory("Export Controls");
    }

    @Test
    void createEntry_ValidEntry_Success() {
        when(glossaryEntryRepository.findByTerm(any())).thenReturn(Optional.empty());
        when(glossaryEntryRepository.save(any(GlossaryEntry.class))).thenReturn(testEntry);

        GlossaryEntry result = glossaryService.createEntry(testEntry);

        assertNotNull(result);
        assertEquals(testEntry.getTerm(), result.getTerm());
        verify(glossaryEntryRepository).save(any(GlossaryEntry.class));
    }

    @Test
    void createEntry_DuplicateTerm_ThrowsException() {
        when(glossaryEntryRepository.findByTerm(testEntry.getTerm())).thenReturn(Optional.of(testEntry));

        GlossaryException exception = assertThrows(GlossaryException.class, () -> {
            glossaryService.createEntry(testEntry);
        });

        assertEquals(GlossaryException.ErrorCodes.DUPLICATE_TERM, exception.getErrorCode());
        verify(glossaryEntryRepository, never()).save(any(GlossaryEntry.class));
    }

    @Test
    void createEntry_NullTerm_ThrowsException() {
        testEntry.setTerm(null);

        GlossaryException exception = assertThrows(GlossaryException.class, () -> {
            glossaryService.createEntry(testEntry);
        });

        assertEquals(GlossaryException.ErrorCodes.MISSING_REQUIRED_FIELD, exception.getErrorCode());
        verify(glossaryEntryRepository, never()).save(any(GlossaryEntry.class));
    }

    @Test
    void createEntry_InvalidCategory_ThrowsException() {
        testEntry.setCategory("Invalid Category");

        GlossaryException exception = assertThrows(GlossaryException.class, () -> {
            glossaryService.createEntry(testEntry);
        });

        assertEquals(GlossaryException.ErrorCodes.INVALID_CATEGORY, exception.getErrorCode());
        verify(glossaryEntryRepository, never()).save(any(GlossaryEntry.class));
    }

    @Test
    void updateEntry_ExistingEntry_Success() {
        String id = "1";
        GlossaryEntry updatedEntry = new GlossaryEntry();
        updatedEntry.setTerm("Updated ECCN");
        updatedEntry.setDefinition("Updated Definition");
        updatedEntry.setCategory("Export Controls");

        when(glossaryEntryRepository.findById(id)).thenReturn(Optional.of(testEntry));
        when(glossaryEntryRepository.findByTerm(any())).thenReturn(Optional.empty());
        when(glossaryEntryRepository.save(any(GlossaryEntry.class))).thenReturn(updatedEntry);

        GlossaryEntry result = glossaryService.updateEntry(id, updatedEntry);

        assertNotNull(result);
        assertEquals(updatedEntry.getTerm(), result.getTerm());
        verify(glossaryEntryRepository).save(any(GlossaryEntry.class));
    }

    @Test
    void updateEntry_NonExistentEntry_ThrowsException() {
        String id = "nonexistent";
        when(glossaryEntryRepository.findById(id)).thenReturn(Optional.empty());

        GlossaryException exception = assertThrows(GlossaryException.class, () -> {
            glossaryService.updateEntry(id, testEntry);
        });

        assertEquals(GlossaryException.ErrorCodes.TERM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getByTerm_ExistingTerm_ReturnsEntry() {
        String term = "ECCN";
        when(glossaryEntryRepository.findByTerm(term)).thenReturn(Optional.of(testEntry));

        GlossaryEntry result = glossaryService.getByTerm(term);

        assertNotNull(result);
        assertEquals(term, result.getTerm());
    }

    @Test
    void getByTerm_NonExistentTerm_ThrowsException() {
        String term = "nonexistent";
        when(glossaryEntryRepository.findByTerm(term)).thenReturn(Optional.empty());

        GlossaryException exception = assertThrows(GlossaryException.class, () -> {
            glossaryService.getByTerm(term);
        });

        assertEquals(GlossaryException.ErrorCodes.TERM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findByCategory_ValidCategory_ReturnsEntries() {
        String category = "Export Controls";
        List<GlossaryEntry> entries = Arrays.asList(testEntry);
        when(glossaryEntryRepository.findByCategory(category)).thenReturn(entries);

        List<GlossaryEntry> results = glossaryService.findByCategory(category);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(category, results.get(0).getCategory());
    }

    @Test
    void findByCategory_InvalidCategory_ThrowsException() {
        String category = "Invalid Category";

        GlossaryException exception = assertThrows(GlossaryException.class, () -> {
            glossaryService.findByCategory(category);
        });

        assertEquals(GlossaryException.ErrorCodes.INVALID_CATEGORY, exception.getErrorCode());
    }

    @Test
    void importBulkEntries_ValidEntries_Success() {
        List<GlossaryEntry> entries = Arrays.asList(testEntry);
        when(glossaryEntryRepository.findByTerm(any())).thenReturn(Optional.empty());
        when(glossaryEntryRepository.saveAll(any())).thenReturn(entries);

        assertDoesNotThrow(() -> glossaryService.importBulkEntries(entries));
        verify(glossaryEntryRepository).saveAll(any());
    }

    @Test
    void importBulkEntries_DuplicateTerm_ThrowsException() {
        List<GlossaryEntry> entries = Arrays.asList(testEntry);
        when(glossaryEntryRepository.findByTerm(any())).thenReturn(Optional.of(testEntry));

        GlossaryException exception = assertThrows(GlossaryException.class, () -> {
            glossaryService.importBulkEntries(entries);
        });

        assertEquals(GlossaryException.ErrorCodes.BULK_IMPORT_ERROR, exception.getErrorCode());
        verify(glossaryEntryRepository, never()).saveAll(any());
    }

    @Test
    void deleteEntry_ExistingId_Success() {
        String id = "1";
        when(glossaryEntryRepository.existsById(id)).thenReturn(true);
        doNothing().when(glossaryEntryRepository).deleteById(id);

        assertDoesNotThrow(() -> glossaryService.deleteEntry(id));
        verify(glossaryEntryRepository).deleteById(id);
    }

    @Test
    void deleteEntry_NonExistentId_ThrowsException() {
        String id = "nonexistent";
        when(glossaryEntryRepository.existsById(id)).thenReturn(false);

        GlossaryException exception = assertThrows(GlossaryException.class, () -> {
            glossaryService.deleteEntry(id);
        });

        assertEquals(GlossaryException.ErrorCodes.TERM_NOT_FOUND, exception.getErrorCode());
        verify(glossaryEntryRepository, never()).deleteById(any());
    }
}