package com.aciworldwide.eccn_management_service.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "export_controls")
@Data
@NoArgsConstructor
public class ExportControl {
    @Id
    private String id;
    private String moduleName;
    private String earClassification;
    private Map<String, String> jurisdictionClassifications; // Key: jurisdiction, Value: classification
    private List<String> conflictingJurisdictions;
    private String unifiedClassification;
    private List<String> complianceRequirements;
    private boolean requiresSpecialHandling = false;

    public ExportControl(String moduleName, String earClassification, 
                        Map<String, String> jurisdictionClassifications) {
        this.moduleName = moduleName;
        this.earClassification = earClassification;
        this.jurisdictionClassifications = jurisdictionClassifications;
        analyzeConflicts();
        determineUnifiedClassification();
    }

    public void analyzeConflicts() {
        // Identify jurisdictions with classifications that conflict with EAR
        this.conflictingJurisdictions = jurisdictionClassifications.entrySet().stream()
            .filter(entry -> !entry.getValue().equals(earClassification))
            .map(Map.Entry::getKey)
            .toList();
    }

    public void determineUnifiedClassification() {
        // Use the most restrictive classification across all jurisdictions
        this.unifiedClassification = jurisdictionClassifications.values().stream()
            .min(String::compareTo)
            .orElse(earClassification);
    }
}