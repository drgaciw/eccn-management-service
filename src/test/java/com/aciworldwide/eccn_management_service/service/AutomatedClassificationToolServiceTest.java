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
    private AutomatedClassificationToolService.AIModel aiModel;

    @Mock
    private Product product;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service.integrateAIModel(aiModel);
        
        AutomatedClassificationToolService.ModuleAnalysis analysis = new AutomatedClassificationToolService.ModuleAnalysis();
        analysis.setEncryptionLibraries(Collections.singletonList("AES"));
        analysis.setHasPaymentProcessing(false);
        
        when(classificationHistoryRepository.findByModuleName("module"))
            .thenReturn(Collections.singletonList(analysis));
    }

    @Test
    void testValidateClassification() {
        // Setup
        when(cryptoClassificationService.classifyCryptography(anyInt(), any(), anyBoolean()))
            .thenReturn("5D002");

        // Test
        boolean result = service.validateClassification("module", "5D002");
        assertTrue(result);
    }

    @Test
    void testSuggestECCN() {
        // Setup
        Map<String, Double> expectedSuggestions = Map.of("5D002", 0.8);
        when(aiModel.suggestClassifications(any(), anyBoolean()))
            .thenReturn(expectedSuggestions);

        // Test
        Map<String, Double> suggestions = service.suggestECCN("module");
        assertNotNull(suggestions);
        assertEquals(expectedSuggestions, suggestions);
    }

    @Test
    void testDetermineClassification() {
        // Setup
        when(cryptoClassificationService.classifyCryptography(anyInt(), any(), anyBoolean()))
            .thenReturn("5D002");

        // Test
        String result = service.determineClassification(Collections.singletonList("AES"), false);
        assertNotNull(result);
        assertEquals("5D002", result);
    }

    @Test
    void testGetAlgorithmForLibrary() {
        // Setup
        when(cryptoClassificationService.getLibraryAlgorithm(anyString(), anyString()))
            .thenReturn(CryptoClassificationService.Algorithm.AES);

        // Test
        CryptoClassificationService.Algorithm algorithm = service.getAlgorithmForLibrary("lib");
        assertNotNull(algorithm);
        assertEquals(CryptoClassificationService.Algorithm.AES, algorithm);
    }

    @Test
    void testIsLibraryMassMarket() {
        // Setup
        when(cryptoClassificationService.isLibraryMassMarket(anyString(), anyString()))
            .thenReturn(true);

        // Test
        boolean result = service.isLibraryMassMarket("lib");
        assertTrue(result);
    }

    @Test
    void testGetLibraryKeyLength() {
        // Setup
        when(cryptoClassificationService.getLibraryKeyLength(anyString(), anyString()))
            .thenReturn(256);

        // Test
        int keyLength = service.getLibraryKeyLength("lib");
        assertEquals(256, keyLength);
    }
}