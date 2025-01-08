package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.model.DocumentVersion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends MongoRepository<DocumentVersion, String> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(String documentId);
    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(String documentId, int versionNumber);
}