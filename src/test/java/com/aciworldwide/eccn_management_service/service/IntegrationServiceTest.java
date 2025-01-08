package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.Product;
import com.aciworldwide.eccn_management_service.model.RiskAssessment;
import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.exception.IntegrationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {

    @Mock
    private IntegrationService integrationService;

    @Test
    void testSyncProductWithPDH() throws IntegrationException {
        Product product = new Product();
        doNothing().when(integrationService).syncProductWithPDH(product, IntegrationService.SyncDirection.TO_PDH);
        
        integrationService.syncProductWithPDH(product, IntegrationService.SyncDirection.TO_PDH);
        verify(integrationService, times(1)).syncProductWithPDH(product, IntegrationService.SyncDirection.TO_PDH);
    }

    @Test
    void testGetProductUpdatesFromPDH() throws IntegrationException {
        Product product = new Product();
        when(integrationService.getProductUpdatesFromPDH(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(product));
        
        List<Product> result = integrationService.getProductUpdatesFromPDH(LocalDateTime.now());
        assertFalse(result.isEmpty());
        assertEquals(product, result.get(0));
    }

    @Test
    void testPublishEccnToPDH() throws IntegrationException {
        Product product = new Product();
        Eccn eccn = new Eccn();
        doNothing().when(integrationService).publishEccnToPDH(product, eccn);
        
        integrationService.publishEccnToPDH(product, eccn);
        verify(integrationService, times(1)).publishEccnToPDH(product, eccn);
    }

    @Test
    void testGetApprovedSuppliersFromPDH() throws IntegrationException {
        when(integrationService.getApprovedSuppliersFromPDH(any(Product.class)))
            .thenReturn(Collections.singletonList("Supplier1"));
        
        List<String> result = integrationService.getApprovedSuppliersFromPDH(new Product());
        assertFalse(result.isEmpty());
        assertEquals("Supplier1", result.get(0));
    }

    @Test
    void testSyncProductWithSalesforce() throws IntegrationException {
        Product product = new Product();
        doNothing().when(integrationService).syncProductWithSalesforce(product, IntegrationService.SyncDirection.TO_SALESFORCE);
        
        integrationService.syncProductWithSalesforce(product, IntegrationService.SyncDirection.TO_SALESFORCE);
        verify(integrationService, times(1)).syncProductWithSalesforce(product, IntegrationService.SyncDirection.TO_SALESFORCE);
    }

    @Test
    void testPublishEccnToSalesforce() throws IntegrationException {
        Product product = new Product();
        Eccn eccn = new Eccn();
        doNothing().when(integrationService).publishEccnToSalesforce(product, eccn);
        
        integrationService.publishEccnToSalesforce(product, eccn);
        verify(integrationService, times(1)).publishEccnToSalesforce(product, eccn);
    }

    @Test
    void testGetOpportunityUpdatesFromSalesforce() throws IntegrationException {
        Product product = new Product();
        when(integrationService.getOpportunityUpdatesFromSalesforce(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(product));
        
        List<Product> result = integrationService.getOpportunityUpdatesFromSalesforce(LocalDateTime.now());
        assertFalse(result.isEmpty());
        assertEquals(product, result.get(0));
    }

    @Test
    void testCreateComplianceRecordInSalesforce() throws IntegrationException {
        Product product = new Product();
        Eccn eccn = new Eccn();
        RiskAssessment riskAssessment = new RiskAssessment();
        doNothing().when(integrationService).createComplianceRecordInSalesforce(product, eccn, riskAssessment);
        
        integrationService.createComplianceRecordInSalesforce(product, eccn, riskAssessment);
        verify(integrationService, times(1)).createComplianceRecordInSalesforce(product, eccn, riskAssessment);
    }

    @Test
    void testHandleIntegrationError() {
        Exception exception = new Exception("Test error");
        doNothing().when(integrationService).handleIntegrationError(exception, IntegrationService.System.PDH, "test data");
        
        integrationService.handleIntegrationError(exception, IntegrationService.System.PDH, "test data");
        verify(integrationService, times(1)).handleIntegrationError(exception, IntegrationService.System.PDH, "test data");
    }

    @Test
    void testTestConnection() throws IntegrationException {
        when(integrationService.testConnection(IntegrationService.System.PDH)).thenReturn(true);
        
        boolean result = integrationService.testConnection(IntegrationService.System.PDH);
        assertTrue(result);
    }

    @Test
    void testGetIntegrationMapping() throws IntegrationException {
        when(integrationService.getIntegrationMapping(IntegrationService.System.PDH, "Product"))
            .thenReturn("mapping data");
        
        Object result = integrationService.getIntegrationMapping(IntegrationService.System.PDH, "Product");
        assertEquals("mapping data", result);
    }
}