package com.aciworldwide.eccn_management_service;

import com.aciworldwide.eccn_management_service.exception.GlossaryException;
import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import com.aciworldwide.eccn_management_service.repository.GlossaryEntryRepository;
import com.aciworldwide.eccn_management_service.service.GlossaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression test that locks in the unique index on {@code GlossaryEntry.term}
 * and the duplicate-term handling in {@link GlossaryService#createEntry}. Runs
 * against the local MongoDB used by the test profile.
 */
@SpringBootTest
@ActiveProfiles("test")
class GlossaryDuplicateKeyTest {

    @Autowired
    private GlossaryService glossaryService;

    @Autowired
    private GlossaryEntryRepository glossaryEntryRepository;

    @BeforeEach
    void cleanUp() {
        glossaryEntryRepository.deleteAll();
    }

    @Test
    void createEntry_withDuplicateTerm_throwsGlossaryExceptionWithDuplicateTerm() {
        String term = "ECCN";
        glossaryService.createEntry(validEntry(term));

        GlossaryException exception = assertThrows(GlossaryException.class,
                () -> glossaryService.createEntry(validEntry(term)));

        assertEquals(GlossaryException.ErrorCodes.DUPLICATE_TERM, exception.getErrorCode());
    }

    @Test
    void createEntry_withUniqueTerm_succeeds() {
        GlossaryEntry saved = glossaryService.createEntry(validEntry("Mass Market Cryptography"));

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("Mass Market Cryptography", saved.getTerm());
    }

    private GlossaryEntry validEntry(String term) {
        GlossaryEntry entry = new GlossaryEntry();
        entry.setTerm(term);
        entry.setDefinition("Export Control Classification Number definition text");
        entry.setRegulatoryContext("BIS Export Administration Regulations");
        entry.setCategory("Export Controls");
        return entry;
    }
}
