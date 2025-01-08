package com.aciworldwide.eccn_management_service.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EccnClassificationWorkflowService {

    public enum ClassificationRole {
        ENGINEER,
        LEGAL,
        COMPLIANCE
    }

    public enum WorkflowStatus {
        INITIATED,
        IN_PROGRESS,
        ESCALATED,
        COMPLETED,
        REVISION_REQUIRED
    }

    public static class ClassificationRequest {
        private String moduleName;
        private String technicalSpecs;
        private String productDesign;
        private List<ClassificationRole> assignedRoles;
        private WorkflowStatus status;

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getTechnicalSpecs() {
            return technicalSpecs;
        }

        public void setTechnicalSpecs(String technicalSpecs) {
            this.technicalSpecs = technicalSpecs;
        }

        public String getProductDesign() {
            return productDesign;
        }

        public void setProductDesign(String productDesign) {
            this.productDesign = productDesign;
        }

        public List<ClassificationRole> getAssignedRoles() {
            return assignedRoles;
        }

        public void setAssignedRoles(List<ClassificationRole> assignedRoles) {
            this.assignedRoles = assignedRoles;
        }

        public WorkflowStatus getStatus() {
            return status;
        }

        public void setStatus(WorkflowStatus status) {
            this.status = status;
        }
    }

    public ClassificationRequest initiateClassification(String moduleName, String technicalSpecs, String productDesign) {
        ClassificationRequest request = new ClassificationRequest();
        request.setModuleName(moduleName);
        request.setTechnicalSpecs(technicalSpecs);
        request.setProductDesign(productDesign);
        request.setAssignedRoles(List.of(ClassificationRole.ENGINEER));
        request.setStatus(WorkflowStatus.INITIATED);
        return request;
    }

    public ClassificationRequest escalateClassification(ClassificationRequest request, String reason) {
        request.setAssignedRoles(List.of(ClassificationRole.LEGAL, ClassificationRole.COMPLIANCE));
        request.setStatus(WorkflowStatus.ESCALATED);
        // Log escalation reason
        return request;
    }

    public ClassificationRequest completeClassification(ClassificationRequest request, String eccnCode) {
        request.setStatus(WorkflowStatus.COMPLETED);
        // Store ECCN code
        return request;
    }

    public boolean requiresReclassification(ClassificationRequest request, String changeDescription) {
        // Logic to determine if reclassification is needed
        return changeDescription.contains("cryptographic") || 
               changeDescription.contains("security") ||
               changeDescription.contains("export");
    }

    public ClassificationRequest triggerReclassification(ClassificationRequest request) {
        request.setStatus(WorkflowStatus.REVISION_REQUIRED);
        return request;
    }
}