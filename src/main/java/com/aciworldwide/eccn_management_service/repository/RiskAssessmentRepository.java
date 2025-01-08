package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.model.RiskAssessment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RiskAssessmentRepository extends MongoRepository<RiskAssessment, String> {
    List<RiskAssessment> findByModuleName(String moduleName);
    List<RiskAssessment> findByRiskLevel(String riskLevel);
    List<RiskAssessment> findByAssessmentDateBetween(LocalDate startDate, LocalDate endDate);
    List<RiskAssessment> findByNextReviewDateBefore(LocalDate date);
    List<RiskAssessment> findByRequiresFollowUp(boolean requiresFollowUp);
    List<RiskAssessment> findByAssessedBy(String username);
    List<RiskAssessment> findByRestrictedEndUsesContaining(String endUse);
    List<RiskAssessment> findByHighRiskUsersContaining(String user);
    List<RiskAssessment> findByThirdPartyComponentsContaining(String component);
}