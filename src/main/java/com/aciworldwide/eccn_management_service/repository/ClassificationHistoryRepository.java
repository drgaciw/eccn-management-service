package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.service.AutomatedClassificationToolService.ModuleAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ClassificationHistoryRepository extends MongoRepository<ModuleAnalysis, String> {
    List<ModuleAnalysis> findByModuleName(String moduleName);
}