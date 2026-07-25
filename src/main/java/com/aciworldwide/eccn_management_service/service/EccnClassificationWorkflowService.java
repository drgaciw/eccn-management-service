package com.aciworldwide.eccn_management_service.service;

import org.springframework.stereotype.Service;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class EccnClassificationWorkflowService {

    public enum Role {
        SOFTWARE_RELEASE_MANAGER,
        PRODUCT_MANAGER,
        COMPLIANCE_MANAGER,
        AUTOMATED_SYSTEM
    }

    public enum WorkflowStatus {
        RELEASE_PLANNING,           // SRM planning release
        RELEASE_DATA_GATHERING,     // SRM gathering release details
        DRAFT,                      // PM preparing classification request
        INFO_GATHERING,             // PM gathering product information
        PENDING_PM_VALIDATION,      // PM reviewing suggested ECCN
        PENDING_CM_REVIEW,          // Waiting for compliance manager review
        PENDING_CLARIFICATION,      // Waiting for additional info
        AUTOMATED_CLASSIFICATION,    // System processing
        PENDING_CM_APPROVAL,        // CM reviewing suggested ECCN
        PENDING_ADJUSTMENTS,        // Needs classification adjustments
        GENERATING_REPORTS,         // Finalizing and generating reports
        COMPLETED                   // Process finished
    }

    @Data
    public static class ReleaseData {
        private String releaseVersion;
        private LocalDateTime plannedReleaseDate;
        private String changeLogSummary;
        private List<String> modifiedComponents;
        private List<String> newFeatures;
        private List<String> technicalChanges;
        private boolean cryptographyModified;
        private String cryptographyChangeDetails;
        private List<String> releaseDocuments;
        private LocalDateTime compilationDate;
        private String releaseManager;
    }

    @Data
    public static class ProductValidation {
        private boolean technicalSpecsCorrect;
        private boolean intendedUseAccurate;
        private boolean encryptionDetailsComplete;
        private boolean marketInfoAccurate;
        private String validationNotes;
        private LocalDateTime validationDate;
    }

    @Data
    public static class ProductData {
        private String technicalSpecifications;
        private String intendedUse;
        private List<String> features;
        private boolean containsCryptography;
        private String cryptographyDetails;
        private List<String> encryptionAlgorithms;
        private String marketSegment;
        private String developmentStatus;
        private List<String> attachedDocuments;
        private String additionalNotes;
        private ProductValidation validation;
    }

    @Data
    public static class ClassificationRequest {
        private String requestId;
        private String productName;
        private String productVersion;
        private String productManager;
        private String complianceManager;
        private LocalDateTime requestDate;
        private ProductData productData;
        private String suggestedEccn;
        private String finalEccn;
        private String complianceNotes;
        private WorkflowStatus status;
        private Role currentAssignee;
        private List<WorkflowHistory> history;
        private List<ClarificationRequest> clarificationRequests;
        private List<String> generatedReports;
        private ReleaseData releaseData;
        private String softwareReleaseManager;
        
        // New constructor for release-initiated requests
        public static ClassificationRequest fromReleaseData(ReleaseData releaseData) {
            ClassificationRequest request = new ClassificationRequest();
            request.setRequestId("REQ-REL-" + System.currentTimeMillis());
            request.setReleaseData(releaseData);
            request.setSoftwareReleaseManager(releaseData.getReleaseManager());
            request.setProductVersion(releaseData.getReleaseVersion());
            request.setRequestDate(LocalDateTime.now());
            request.setStatus(WorkflowStatus.RELEASE_PLANNING);
            request.setCurrentAssignee(Role.SOFTWARE_RELEASE_MANAGER);
            return request;
        }
    }

    @Data
    public static class ClarificationRequest {
        private LocalDateTime requestDate;
        private String requestedBy;
        private String question;
        private String response;
        private LocalDateTime responseDate;
        private boolean resolved;
    }

    @Data
    public static class WorkflowHistory {
        private LocalDateTime timestamp;
        private Role role;
        private String action;
        private String notes;
        private WorkflowStatus previousStatus;
        private WorkflowStatus newStatus;
    }

    // Compliance Manager Methods

    public ClassificationRequest reviewInitialRequest(
            ClassificationRequest request, 
            boolean isInformationSufficient,
            String notes) {
        if (request.getStatus() != WorkflowStatus.PENDING_CM_REVIEW) {
            throw new IllegalStateException("Request is not pending compliance manager review");
        }

        if (isInformationSufficient) {
            request.setStatus(WorkflowStatus.AUTOMATED_CLASSIFICATION);
            request.setCurrentAssignee(Role.AUTOMATED_SYSTEM);
            addToHistory(request, Role.COMPLIANCE_MANAGER, 
                "Information deemed sufficient, proceeding to automated classification");
        } else {
            request.setStatus(WorkflowStatus.PENDING_CLARIFICATION);
            request.setCurrentAssignee(Role.PRODUCT_MANAGER);
            requestClarification(request, "Initial review requires additional information: " + notes);
        }
        
        request.setComplianceNotes(notes);
        return request;
    }

    public ClassificationRequest reviewSuggestedEccn(
            ClassificationRequest request,
            boolean approved,
            String notes) {
        if (request.getStatus() != WorkflowStatus.PENDING_CM_APPROVAL) {
            throw new IllegalStateException("Request is not pending ECCN approval");
        }

        if (approved) {
            request.setStatus(WorkflowStatus.GENERATING_REPORTS);
            request.setCurrentAssignee(Role.AUTOMATED_SYSTEM);
            request.setFinalEccn(request.getSuggestedEccn());
            addToHistory(request, Role.COMPLIANCE_MANAGER, 
                "Approved ECCN: " + request.getSuggestedEccn() + "\nNotes: " + notes);
        } else {
            request.setStatus(WorkflowStatus.PENDING_ADJUSTMENTS);
            request.setCurrentAssignee(Role.AUTOMATED_SYSTEM);
            addToHistory(request, Role.COMPLIANCE_MANAGER,
                "Requested adjustments to classification\nNotes: " + notes);
        }

        request.setComplianceNotes(notes);
        return request;
    }

    public ClassificationRequest requestClarification(
            ClassificationRequest request,
            String question) {
        ClarificationRequest clarification = new ClarificationRequest();
        clarification.setRequestDate(LocalDateTime.now());
        clarification.setRequestedBy(request.getComplianceManager());
        clarification.setQuestion(question);
        clarification.setResolved(false);

        if (request.getClarificationRequests() == null) {
            request.setClarificationRequests(new ArrayList<>());
        }
        request.getClarificationRequests().add(clarification);
        
        request.setStatus(WorkflowStatus.PENDING_CLARIFICATION);
        request.setCurrentAssignee(Role.PRODUCT_MANAGER);
        
        addToHistory(request, Role.COMPLIANCE_MANAGER,
            "Requested clarification: " + question);
        return request;
    }

    // Software Release Manager Methods

    public ClassificationRequest initializeReleaseClassification(String releaseVersion, LocalDateTime plannedReleaseDate) {
        ReleaseData releaseData = new ReleaseData();
        releaseData.setReleaseVersion(releaseVersion);
        releaseData.setPlannedReleaseDate(plannedReleaseDate);
        releaseData.setCompilationDate(LocalDateTime.now());
        
        ClassificationRequest request = ClassificationRequest.fromReleaseData(releaseData);
        addToHistory(request, Role.SOFTWARE_RELEASE_MANAGER, 
            "Release classification process initiated for version " + releaseVersion);
        return request;
    }

    public ClassificationRequest compileReleaseDetails(
            ClassificationRequest request,
            ReleaseData releaseData) {
        if (request.getStatus() != WorkflowStatus.RELEASE_PLANNING) {
            throw new IllegalStateException("Request must be in release planning status");
        }

        validateReleaseData(releaseData);
        request.setReleaseData(releaseData);
        request.setStatus(WorkflowStatus.RELEASE_DATA_GATHERING);
        
        addToHistory(request, Role.SOFTWARE_RELEASE_MANAGER,
            "Release details compiled and ready for product manager review");
        return request;
    }

    public ClassificationRequest handoffToProductManager(
            ClassificationRequest request,
            String productManager) {
        if (request.getStatus() != WorkflowStatus.RELEASE_DATA_GATHERING) {
            throw new IllegalStateException("Release data must be gathered first");
        }

        request.setProductManager(productManager);
        request.setStatus(WorkflowStatus.DRAFT);
        request.setCurrentAssignee(Role.PRODUCT_MANAGER);
        
        addToHistory(request, Role.SOFTWARE_RELEASE_MANAGER,
            "Release details handed off to product manager: " + productManager);
        return request;
    }

    // Modified Product Manager Methods

    public ClassificationRequest acceptReleaseData(
            ClassificationRequest request,
            boolean isDataComplete) {
        if (request.getStatus() != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Request must be in draft status");
        }

        if (isDataComplete) {
            request.setStatus(WorkflowStatus.INFO_GATHERING);
            addToHistory(request, Role.PRODUCT_MANAGER,
                "Release data accepted, proceeding with classification");
        } else {
            request.setStatus(WorkflowStatus.RELEASE_DATA_GATHERING);
            request.setCurrentAssignee(Role.SOFTWARE_RELEASE_MANAGER);
            addToHistory(request, Role.PRODUCT_MANAGER,
                "Release data incomplete, returned to release manager");
        }
        
        return request;
    }

    public ClassificationRequest gatherProductInformation(
            ClassificationRequest request,
            ProductData productData) {
        if (request.getStatus() != WorkflowStatus.INFO_GATHERING) {
            throw new IllegalStateException("Request must be in INFO_GATHERING status");
        }

        request.setProductData(productData);
        request.setStatus(WorkflowStatus.PENDING_PM_VALIDATION);
        
        addToHistory(request, Role.PRODUCT_MANAGER, 
            "Product information gathering completed");
        return request;
    }

    public ClassificationRequest submitClassificationRequest(
            ClassificationRequest request,
            String productManager,
            String complianceManager) {
        if (request.getStatus() != WorkflowStatus.INFO_GATHERING) {
            throw new IllegalStateException("Product information must be gathered first");
        }

        validateProductData(request.getProductData());

        request.setProductManager(productManager);
        request.setComplianceManager(complianceManager);
        request.setRequestDate(LocalDateTime.now());
        request.setStatus(WorkflowStatus.PENDING_CM_REVIEW);
        request.setCurrentAssignee(Role.COMPLIANCE_MANAGER);
        request.setClarificationRequests(new ArrayList<>());
        request.setGeneratedReports(new ArrayList<>());
        
        addToHistory(request, Role.PRODUCT_MANAGER, 
            "Classification request submitted with complete product information");
        return request;
    }

    public ClassificationRequest validateSuggestedEccn(
            ClassificationRequest request,
            boolean isEccnCorrect,
            String validationNotes) {
        if (request.getStatus() != WorkflowStatus.PENDING_PM_VALIDATION) {
            throw new IllegalStateException("No ECCN suggestion pending validation");
        }

        if (isEccnCorrect) {
            request.setStatus(WorkflowStatus.PENDING_CM_APPROVAL);
            request.setCurrentAssignee(Role.COMPLIANCE_MANAGER);
            addToHistory(request, Role.PRODUCT_MANAGER,
                "Validated ECCN suggestion as correct: " + request.getSuggestedEccn());
        } else {
            request.setStatus(WorkflowStatus.PENDING_ADJUSTMENTS);
            request.setCurrentAssignee(Role.AUTOMATED_SYSTEM);
            addToHistory(request, Role.PRODUCT_MANAGER,
                "Rejected ECCN suggestion. Notes: " + validationNotes);
        }

        return request;
    }

    public ClassificationRequest provideClarification(
            ClassificationRequest request,
            String response,
            ProductData updatedData) {
        if (request.getStatus() != WorkflowStatus.PENDING_CLARIFICATION) {
            throw new IllegalStateException("No clarification is pending");
        }

        ClarificationRequest currentClarification = getCurrentClarification(request);
        currentClarification.setResponse(response);
        currentClarification.setResponseDate(LocalDateTime.now());
        currentClarification.setResolved(true);

        if (updatedData != null) {
            validateProductData(updatedData);
            request.setProductData(updatedData);
        }

        request.setStatus(WorkflowStatus.PENDING_CM_REVIEW);
        request.setCurrentAssignee(Role.COMPLIANCE_MANAGER);
        
        addToHistory(request, Role.PRODUCT_MANAGER,
            "Provided clarification with updated product information");
        return request;
    }

    // Automated System Methods

    public ClassificationRequest generateClassificationSuggestion(
            ClassificationRequest request) {
        if (request.getStatus() != WorkflowStatus.AUTOMATED_CLASSIFICATION) {
            throw new IllegalStateException("Request is not ready for automated classification");
        }

        // Automated classification logic would go here
        String suggestedEccn = "5D002"; // Example
        
        request.setSuggestedEccn(suggestedEccn);
        request.setStatus(WorkflowStatus.PENDING_CM_APPROVAL);
        request.setCurrentAssignee(Role.COMPLIANCE_MANAGER);
        
        addToHistory(request, Role.AUTOMATED_SYSTEM,
            "Generated ECCN suggestion: " + suggestedEccn);
        return request;
    }

    public ClassificationRequest generateReports(ClassificationRequest request) {
        if (request.getStatus() != WorkflowStatus.GENERATING_REPORTS) {
            throw new IllegalStateException("Request is not ready for report generation");
        }

        // Report generation logic would go here
        List<String> reports = new ArrayList<>();
        reports.add("Classification_Report_" + request.getRequestId() + ".pdf");
        reports.add("Regulatory_Submission_" + request.getRequestId() + ".pdf");
        
        request.setGeneratedReports(reports);
        request.setStatus(WorkflowStatus.COMPLETED);
        
        addToHistory(request, Role.AUTOMATED_SYSTEM,
            "Generated final reports: " + String.join(", ", reports));
        return request;
    }

    // Helper Methods

    /**
     * Validates product data ensuring all required fields are present.
     * @param productData the product data to validate
     * @throws IllegalArgumentException if product data is null or required fields are missing
     */
    private void validateProductData(ProductData productData) {
        if (productData == null) {
            throw new IllegalArgumentException("Product data cannot be null");
        }

        List<String> missingFields = collectMissingProductFields(productData);
        throwIfMissingFields(missingFields, "Required product information missing: ");
    }

    /**
     * Collects all missing required fields from product data.
     * @param productData the product data to check
     * @return list of missing field names
     */
    private List<String> collectMissingProductFields(ProductData productData) {
        List<String> missingFields = new ArrayList<>();
        
        collectIfMissing(missingFields, "Technical Specifications", productData.getTechnicalSpecifications());
        collectIfMissing(missingFields, "Intended Use", productData.getIntendedUse());
        collectIfMissing(missingFields, "Features", productData.getFeatures());
        collectCryptographyDetailsIfMissing(missingFields, productData);
        collectIfMissing(missingFields, "Market Segment", productData.getMarketSegment());
        collectIfMissing(missingFields, "Development Status", productData.getDevelopmentStatus());
        
        return missingFields;
    }

    /**
     * Adds field name to missing fields list if the string value is empty.
     * @param missingFields the list to collect missing field names into
     * @param fieldName the name of the field being checked
     * @param value the value to check
     */
    private void collectIfMissing(List<String> missingFields, String fieldName, String value) {
        if (isEmpty(value)) {
            missingFields.add(fieldName);
        }
    }

    /**
     * Adds field name to missing fields list if the list value is empty.
     * @param missingFields the list to collect missing field names into
     * @param fieldName the name of the field being checked
     * @param value the list value to check
     */
    private void collectIfMissing(List<String> missingFields, String fieldName, List<?> value) {
        if (isEmpty(value)) {
            missingFields.add(fieldName);
        }
    }

    /**
     * Collects cryptography details as missing if cryptography is enabled but details are not provided.
     * @param missingFields the list to collect missing field names into
     * @param productData the product data containing cryptography information
     */
    private void collectCryptographyDetailsIfMissing(List<String> missingFields, ProductData productData) {
        if (productData.isContainsCryptography() && isEmpty(productData.getCryptographyDetails())) {
            missingFields.add("Cryptography Details");
        }
    }

    /**
     * Throws an IllegalArgumentException if there are missing fields.
     * @param missingFields the list of missing field names
     * @param messagePrefix the prefix for the error message
     * @throws IllegalArgumentException if there are missing fields
     */
    private void throwIfMissingFields(List<String> missingFields, String messagePrefix) {
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(messagePrefix + String.join(", ", missingFields));
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private ClarificationRequest getCurrentClarification(ClassificationRequest request) {
        return request.getClarificationRequests().stream()
                .filter(cr -> !cr.isResolved())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No pending clarification request found"));
    }

    private void addToHistory(ClassificationRequest request, Role role, String action) {
        WorkflowHistory entry = new WorkflowHistory();
        entry.setTimestamp(LocalDateTime.now());
        entry.setRole(role);
        entry.setAction(action);
        entry.setPreviousStatus(request.getStatus());
        
        if (request.getHistory() == null) {
            request.setHistory(new ArrayList<>());
        }
        request.getHistory().add(entry);
    }

    /**
     * Validates release data ensuring all required fields are present.
     * @param releaseData the release data to validate
     * @throws IllegalArgumentException if release data is null or required fields are missing
     */
    private void validateReleaseData(ReleaseData releaseData) {
        if (releaseData == null) {
            throw new IllegalArgumentException("Release data cannot be null");
        }

        List<String> missingFields = collectMissingReleaseFields(releaseData);
        throwIfMissingFields(missingFields, "Required release information missing: ");
    }

    /**
     * Collects all missing required fields from release data.
     * @param releaseData the release data to check
     * @return list of missing field names
     */
    private List<String> collectMissingReleaseFields(ReleaseData releaseData) {
        List<String> missingFields = new ArrayList<>();
        
        collectIfMissing(missingFields, "Release Version", releaseData.getReleaseVersion());
        collectIfDateMissing(missingFields, "Planned Release Date", releaseData.getPlannedReleaseDate());
        collectIfMissing(missingFields, "Change Log Summary", releaseData.getChangeLogSummary());
        collectIfMissing(missingFields, "Modified Components", releaseData.getModifiedComponents());
        collectCryptographyChangeDetailsIfMissing(missingFields, releaseData);
        
        return missingFields;
    }

    /**
     * Adds field name to missing fields list if the date value is null.
     * @param missingFields the list to collect missing field names into
     * @param fieldName the name of the field being checked
     * @param value the date value to check
     */
    private void collectIfDateMissing(List<String> missingFields, String fieldName, LocalDateTime value) {
        if (value == null) {
            missingFields.add(fieldName);
        }
    }

    /**
     * Collects cryptography change details as missing if cryptography was modified but details are not provided.
     * @param missingFields the list to collect missing field names into
     * @param releaseData the release data containing cryptography modification information
     */
    private void collectCryptographyChangeDetailsIfMissing(List<String> missingFields, ReleaseData releaseData) {
        if (releaseData.isCryptographyModified() && isEmpty(releaseData.getCryptographyChangeDetails())) {
            missingFields.add("Cryptography Change Details");
        }
    }
}
