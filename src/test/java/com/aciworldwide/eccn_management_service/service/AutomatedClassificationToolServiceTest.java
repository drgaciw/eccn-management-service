package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.Product;
import com.aciworldwide.eccn_management_service.repository.ClassificationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutomatedClassificationToolServiceTest {

    @InjectMocks
    private AutomatedClassificationToolService service;

    @Mock
    private CryptoClassificationService cryptoClassificationService;

    @Mock
    private ClassificationHistoryRepository classificationHistoryRepository;

    @Mock
    private Product product;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        AutomatedClassificationToolService.ModuleAnalysis analysis = new AutomatedClassificationToolService.ModuleAnalysis();
        analysis.setEncryptionLibraries(Collections.singletonList("AES"));
        analysis.setHasPaymentProcessing(false);

        when(classificationHistoryRepository.findByModuleName("module"))
            .thenReturn(Collections.singletonList(analysis));
    }

    @Test
    void testValidateClassification() {
        when(cryptoClassificationService.classifyCryptography(anyInt(), any(), anyBoolean()))
            .thenReturn("5D002");

        boolean result = service.validateClassification("module", "5D002");
        assertTrue(result);
    }

    @Test
    void testSuggestECCN() {
        Map<String, Double> suggestions = service.suggestECCN("module");
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty(),
            "Without ChatClient configured, suggestECCN should return empty map");
    }

    @Test
    void testDetermineClassification() {
        when(cryptoClassificationService.classifyCryptography(anyInt(), any(), anyBoolean()))
            .thenReturn("5D002");

        String result = service.determineClassification(Collections.singletonList("AES"), false);
        assertNotNull(result);
        assertEquals("5D002", result);
    }

    @Test
    void testGetAlgorithmForLibrary() {
        when(cryptoClassificationService.getLibraryAlgorithm(anyString(), anyString()))
            .thenReturn(CryptoClassificationService.Algorithm.AES);

        CryptoClassificationService.Algorithm algorithm = service.getAlgorithmForLibrary("lib");
        assertNotNull(algorithm);
        assertEquals(CryptoClassificationService.Algorithm.AES, algorithm);
    }

    @Test
    void testIsLibraryMassMarket() {
        when(cryptoClassificationService.isLibraryMassMarket(anyString(), anyString()))
            .thenReturn(true);

        boolean result = service.isLibraryMassMarket("lib");
        assertTrue(result);
    }

    @Test
    void testGetLibraryKeyLength() {
        when(cryptoClassificationService.getLibraryKeyLength(anyString(), anyString()))
            .thenReturn(256);

        int keyLength = service.getLibraryKeyLength("lib");
        assertEquals(256, keyLength);
    }
}
