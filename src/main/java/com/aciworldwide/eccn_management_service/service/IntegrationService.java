package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.Product;
import com.aciworldwide.eccn_management_service.model.RiskAssessment;
import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.exception.IntegrationException;
import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationService {

    enum SyncDirection {
        TO_PDH, FROM_PDH, BIDIRECTIONAL,
        TO_SALESFORCE, FROM_SALESFORCE
    }

    enum System {
        PDH, SALESFORCE
    }

    // Oracle PDH Integration Methods
    void syncProductWithPDH(Product product, SyncDirection direction) throws IntegrationException;
    List<Product> getProductUpdatesFromPDH(LocalDateTime lastSyncTime) throws IntegrationException;
    void publishEccnToPDH(Product product, Eccn assignedEccn) throws IntegrationException;
    List<String> getApprovedSuppliersFromPDH(Product product) throws IntegrationException;

    // Salesforce CPQ/CRM Integration Methods
    void syncProductWithSalesforce(Product product, SyncDirection direction) throws IntegrationException;
    void publishEccnToSalesforce(Product product, Eccn assignedEccn) throws IntegrationException;
    List<Product> getOpportunityUpdatesFromSalesforce(LocalDateTime lastSyncTime) throws IntegrationException;
    void createComplianceRecordInSalesforce(Product product, Eccn eccn, RiskAssessment riskAssessment) throws IntegrationException;

    // General Integration Methods
    void handleIntegrationError(Exception error, System system, Object data);
    boolean testConnection(System system) throws IntegrationException;
    Object getIntegrationMapping(System system, String objectType) throws IntegrationException;
}