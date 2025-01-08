package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.RiskAssessment;
import com.aciworldwide.eccn_management_service.repository.RiskAssessmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;

    public RiskAssessmentService(RiskAssessmentRepository riskAssessmentRepository) {
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    @Transactional
    public RiskAssessment createRiskAssessment(String moduleName, List<String> restrictedEndUses,
                                             List<String> highRiskUsers, List<String> thirdPartyComponents,
                                             String assessedBy) {
        RiskAssessment assessment = new RiskAssessment(moduleName, restrictedEndUses,
                highRiskUsers, thirdPartyComponents, assessedBy);
        return riskAssessmentRepository.save(assessment);
    }

    public List<RiskAssessment> getAssessmentsByModule(String moduleName) {
        return riskAssessmentRepository.findByModuleName(moduleName);
    }

    public List<RiskAssessment> getHighRiskAssessments() {
        return riskAssessmentRepository.findByRiskLevel("HIGH");
    }

    public List<RiskAssessment> getAssessmentsRequiringReview() {
        return riskAssessmentRepository.findByNextReviewDateBefore(LocalDate.now());
    }

    @Transactional
    public void flagForFollowUp(String assessmentId) {
        riskAssessmentRepository.findById(assessmentId).ifPresent(assessment -> {
            assessment.setRequiresFollowUp(true);
            riskAssessmentRepository.save(assessment);
        });
    }

    public List<RiskAssessment> searchAssessmentsByEndUse(String endUse) {
        return riskAssessmentRepository.findByRestrictedEndUsesContaining(endUse);
    }

    public List<RiskAssessment> searchAssessmentsByThirdPartyComponent(String component) {
        return riskAssessmentRepository.findByThirdPartyComponentsContaining(component);
    }

    @Transactional
    public void updateMitigationActions(String assessmentId, List<String> actions) {
        riskAssessmentRepository.findById(assessmentId).ifPresent(assessment -> {
            assessment.setMitigationActions(actions);
            riskAssessmentRepository.save(assessment);
        });
    }

    public List<RiskAssessment> getAssessmentsByRiskLevel(String riskLevel) {
        return riskAssessmentRepository.findByRiskLevel(riskLevel);
    }

    @Transactional
    public void scheduleNextReview(String assessmentId, int months) {
        riskAssessmentRepository.findById(assessmentId).ifPresent(assessment -> {
            assessment.setNextReviewDate(LocalDate.now().plusMonths(months));
            riskAssessmentRepository.save(assessment);
        });
    }
}