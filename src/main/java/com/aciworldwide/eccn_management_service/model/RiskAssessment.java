package com.aciworldwide.eccn_management_service.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "risk_assessments")
@Data
@NoArgsConstructor
public class RiskAssessment {
    @Id
    private String id;
    private String moduleName;
    private List<String> restrictedEndUses;
    private List<String> highRiskUsers;
    private List<String> thirdPartyComponents;
    private int riskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private List<String> mitigationActions;
    private LocalDate assessmentDate;
    private LocalDate nextReviewDate;
    private String assessedBy;
    private boolean requiresFollowUp = false;

    public RiskAssessment(String moduleName, List<String> restrictedEndUses,
                         List<String> highRiskUsers, List<String> thirdPartyComponents,
                         String assessedBy) {
        this.moduleName = moduleName;
        this.restrictedEndUses = restrictedEndUses;
        this.highRiskUsers = highRiskUsers;
        this.thirdPartyComponents = thirdPartyComponents;
        this.assessmentDate = LocalDate.now();
        this.nextReviewDate = this.assessmentDate.plusMonths(6); // 6-month review cycle
        this.assessedBy = assessedBy;
        calculateRiskScore();
    }

    private void calculateRiskScore() {
        int score = 0;
        score += restrictedEndUses.size() * 10;
        score += highRiskUsers.size() * 5;
        score += thirdPartyComponents.size() * 3;
        
        if (score <= 10) {
            riskLevel = "LOW";
        } else if (score <= 30) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "HIGH";
        }
        this.riskScore = score;
    }
}