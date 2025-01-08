package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.model.ExportControl;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportControlRepository extends MongoRepository<ExportControl, String> {
    List<ExportControl> findByModuleName(String moduleName);
    List<ExportControl> findByUnifiedClassification(String classification);
    List<ExportControl> findByConflictingJurisdictionsContaining(String jurisdiction);
    List<ExportControl> findByRequiresSpecialHandling(boolean requiresSpecialHandling);
    @org.springframework.data.mongodb.repository.Query("{ 'jurisdictionClassifications.?0': { $exists: true } }")
    List<ExportControl> findByJurisdictionClassificationsKey(String jurisdiction);
    List<ExportControl> findByComplianceRequirementsContaining(String requirement);
}