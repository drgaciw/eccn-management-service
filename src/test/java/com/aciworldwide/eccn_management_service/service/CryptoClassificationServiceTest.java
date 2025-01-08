package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CryptoClassificationServiceTest {

    @InjectMocks
    private CryptoClassificationService service;

    @Mock
    private Product product;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testClassifyCryptography() {
        // Test mass market classification
        String result = service.classifyCryptography(128, CryptoClassificationService.Algorithm.AES, true);
        assertEquals("ECCN 5D992", result, "Expected mass market classification");

        // Test controlled encryption
        result = service.classifyCryptography(256, CryptoClassificationService.Algorithm.ECC, false);
        assertEquals("ECCN 5D002", result, "Expected controlled encryption classification");

        // Test unclassified
        result = service.classifyCryptography(64, CryptoClassificationService.Algorithm.BLOWFISH, false);
        assertEquals("ECCN 5D992", result, "Expected mass market classification for key length 64 and BLOWFISH algorithm");
    }

    @Test
    void testClassifyCryptoAlgorithm() {
        // Test weak mode
        String result = service.classifyCryptoAlgorithm(
            CryptoClassificationService.Algorithm.AES, 
            256, 
            CryptoClassificationService.Mode.ECB
        );
        assertEquals("ECCN 5D002", result, "Expected controlled classification for weak mode");

        // Test strong mode
        result = service.classifyCryptoAlgorithm(
            CryptoClassificationService.Algorithm.AES, 
            256, 
            CryptoClassificationService.Mode.CBC
        );
        assertTrue(result.contains("ECCN 5D002") || result.contains("ECCN 5D992"), 
            "Expected either controlled or mass market classification for strong mode");
    }

    @Test
    void testClassifyCryptoImplementation() {
        // Test with default values
        String result = service.classifyCryptoImplementation("sourceCode", "Java");
        assertNotNull(result, "Expected non-null classification result");
    }

    @Test
    void testAssessDeMinimis() {
        // Test qualifying product
        when(product.getUsContentPercentage()).thenReturn(20.0);
        when(product.isMilitaryUse()).thenReturn(false);
        when(product.isControlledCountry()).thenReturn(false);
        assertTrue(service.assessDeMinimis(product), "Expected to qualify for de minimis");

        // Test non-qualifying product
        when(product.getUsContentPercentage()).thenReturn(30.0);
        assertFalse(service.assessDeMinimis(product), "Expected not to qualify for de minimis");
    }

    @Test
    void testGetEncryptionRegistrationNumber() {
        // Test product requiring ERN
        when(product.isEncryptionEnabled()).thenReturn(true);
        when(product.getUsContentPercentage()).thenReturn(30.0);
        when(product.isMilitaryUse()).thenReturn(false);
        when(product.isControlledCountry()).thenReturn(false);
        when(product.getId()).thenReturn("123");
        
        String ern = service.getEncryptionRegistrationNumber(product);
        assertNotNull(ern, "Expected non-null ERN");
        assertTrue(ern.startsWith("ERN-"), "Expected ERN to start with 'ERN-'");

        // Test product not requiring ERN
        when(product.getUsContentPercentage()).thenReturn(20.0);
        assertNull(service.getEncryptionRegistrationNumber(product), 
            "Expected null ERN for non-qualifying product");
    }

    @Test
    void testAnalyzeCryptoLibrary() {
        // Test with default values
        String result = service.analyzeCryptoLibrary("library", "1.0");
        assertNotNull(result, "Expected non-null analysis result");
    }

    @Test
    void testQualifiesForMassMarket() {
        // Test qualifying criteria
        assertTrue(service.qualifiesForMassMarket(128, CryptoClassificationService.Algorithm.AES),
            "Expected to qualify for mass market");
        
        // Test non-qualifying criteria
        assertFalse(service.qualifiesForMassMarket(256, CryptoClassificationService.Algorithm.AES),
            "Expected not to qualify for mass market");
    }

    @Test
    void testQualifiesForControlledEncryption() {
        // Test qualifying criteria
        assertTrue(service.qualifiesForControlledEncryption(256, CryptoClassificationService.Algorithm.ECC),
            "Expected to qualify for controlled encryption");
        
        // Test non-qualifying criteria
        assertFalse(service.qualifiesForControlledEncryption(128, CryptoClassificationService.Algorithm.AES),
            "Expected not to qualify for controlled encryption");
    }

    @Test
    void testIsRestrictedAlgorithm() {
        // Test restricted algorithms
        assertTrue(service.isRestrictedAlgorithm(CryptoClassificationService.Algorithm.SHA3),
            "Expected SHA3 to be restricted");
        
        // Test non-restricted algorithms
        assertFalse(service.isRestrictedAlgorithm(CryptoClassificationService.Algorithm.AES),
            "Expected AES not to be restricted");
    }

    @Test
    void testIsWeakMode() {
        // Test weak mode
        assertTrue(service.isWeakMode(CryptoClassificationService.Mode.ECB),
            "Expected ECB to be weak mode");
        
        // Test strong mode
        assertFalse(service.isWeakMode(CryptoClassificationService.Mode.CBC),
            "Expected CBC not to be weak mode");
    }
}