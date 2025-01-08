package com.aciworldwide.eccn_management_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document(collection = "eccn_codes")
public class Eccn {
    @Id
    private String id;
    private String commodityCode;
    private String description;
    private String category;
    
    // New fields for encryption-related classifications
    private boolean encryptionRelated;
    private String encryptionControlNumber; // e.g., 5D002
    private List<String> encryptionAlgorithms;
    
    // Fields for financial transaction software
    private boolean financialSoftware;
    private List<String> supportedPaymentMethods;
    private boolean fraudDetectionCapabilities;
    
    // Fields for data analytics
    private boolean dataAnalytics;
    private List<String> analyticsCapabilities;
    
    // EAR controls and exceptions
    private List<String> applicableEARControls;
    private List<String> exceptions;

    // ECCN lifecycle management
    private boolean deprecated;
    private String deprecationReason;
    private String replacementEccnId;
    private Date createdDate;
    private Date modifiedDate;
    private List<String> relatedEccns;
    private List<EccnHistoryEntry> history;

    @Getter
    @Setter
    public static class EccnHistoryEntry {
        private Date changeDate;
        private String changedBy;
        private String changeDescription;
    }
}