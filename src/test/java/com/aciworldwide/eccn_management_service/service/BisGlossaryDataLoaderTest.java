package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BisGlossaryDataLoaderTest {

    @Mock
    private GlossaryService glossaryService;

    @InjectMocks
    private BisGlossaryDataLoader bisGlossaryDataLoader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadInitialData_WhenDatabaseEmpty_ShouldLoadEntries() {
        when(glossaryService.getAllEntries()).thenReturn(Collections.emptyList());

        bisGlossaryDataLoader.loadInitialData();

        verify(glossaryService).importBulkEntries(anyList());
    }

    @Test
    void loadInitialData_WhenDatabaseNotEmpty_ShouldNotLoadEntries() {
        GlossaryEntry existingEntry = new GlossaryEntry();
        existingEntry.setTerm("ECCN");
        when(glossaryService.getAllEntries()).thenReturn(List.of(existingEntry));

        bisGlossaryDataLoader.loadInitialData();

        verify(glossaryService, never()).importBulkEntries(anyList());
    }

    @Test
    void loadInitialData_WhenImportFails_ShouldNotCrash() {
        when(glossaryService.getAllEntries()).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Test exception")).when(glossaryService).importBulkEntries(anyList());

        assertDoesNotThrow(() -> bisGlossaryDataLoader.loadInitialData());
    }

    @Test
    void createEntry_ShouldReturnValidGlossaryEntry() {
        String term = "TEST";
        String definition = "Test Definition";
        String context = "Test Context";
        String category = "Test Category";

        GlossaryEntry entry = bisGlossaryDataLoader.createEntry(term, definition, context, category);

        assertNotNull(entry);
        assertEquals(term, entry.getTerm());
        assertEquals(definition, entry.getDefinition());
        assertEquals(context, entry.getRegulatoryContext());
        assertEquals(category, entry.getCategory());
    }
}