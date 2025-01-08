package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.model.DocumentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRecordRepository extends MongoRepository<DocumentRecord, UUID> {
    List<DocumentRecord> findByDocumentType(String documentType);
    List<DocumentRecord> findByAssociatedModule(String moduleName);
    List<DocumentRecord> findByEccnClassification(String eccnClassification);
    List<DocumentRecord> findByCreationDateBetween(LocalDate startDate, LocalDate endDate);
    List<DocumentRecord> findByExpirationDateBefore(LocalDate date);
    List<DocumentRecord> findByDocumentNameContainingIgnoreCase(String searchTerm);
    List<DocumentRecord> findByCreatedBy(String username);
    List<DocumentRecord> findByArchived(boolean archived);
    List<DocumentRecord> findByDocumentTypeAndAssociatedModule(String documentType, String moduleName);
}