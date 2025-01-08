package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.ExportControl;
import com.aciworldwide.eccn_management_service.repository.ExportControlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ExportControlService {

    private final ExportControlRepository exportControlRepository;

    public ExportControlService(ExportControlRepository exportControlRepository) {
        this.exportControlRepository = exportControlRepository;
    }

    @Transactional
    public ExportControl createExportControl(String moduleName, String earClassification,
                                            Map<String, String> jurisdictionClassifications) {
        ExportControl control = new ExportControl(moduleName, earClassification, jurisdictionClassifications);
        return exportControlRepository.save(control);
    }

    public List<ExportControl> getExportControlsByModule(String moduleName) {
        return exportControlRepository.findByModuleName(moduleName);
    }

    public List<ExportControl> getExportControlsByJurisdiction(String jurisdiction) {
        return exportControlRepository.findByJurisdictionClassificationsKey(jurisdiction);
    }

    public List<ExportControl> getExportControlsWithConflicts() {
        return exportControlRepository.findByRequiresSpecialHandling(true);
    }

    @Transactional
    public void addComplianceRequirement(String controlId, String requirement) {
        exportControlRepository.findById(controlId).ifPresent(control -> {
            control.getComplianceRequirements().add(requirement);
            exportControlRepository.save(control);
        });
    }

    public List<ExportControl> getExportControlsByUnifiedClassification(String classification) {
        return exportControlRepository.findByUnifiedClassification(classification);
    }

    @Transactional
    public void flagForSpecialHandling(String controlId) {
        exportControlRepository.findById(controlId).ifPresent(control -> {
            control.setRequiresSpecialHandling(true);
            exportControlRepository.save(control);
        });
    }

    public List<ExportControl> searchExportControlsByRequirement(String requirement) {
        return exportControlRepository.findByComplianceRequirementsContaining(requirement);
    }

    @Transactional
    public void updateJurisdictionClassification(String controlId, String jurisdiction, String classification) {
        exportControlRepository.findById(controlId).ifPresent(control -> {
            control.getJurisdictionClassifications().put(jurisdiction, classification);
            control.analyzeConflicts();
            control.determineUnifiedClassification();
            exportControlRepository.save(control);
        });
    }
}