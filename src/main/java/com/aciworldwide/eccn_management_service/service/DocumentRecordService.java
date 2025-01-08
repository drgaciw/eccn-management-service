package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.DocumentRecord;
import com.aciworldwide.eccn_management_service.model.DocumentVersion;
import com.aciworldwide.eccn_management_service.repository.DocumentRecordRepository;
import com.aciworldwide.eccn_management_service.repository.DocumentVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

@Service
public class DocumentRecordService {

    private final DocumentRecordRepository documentRecordRepository;
    private final DocumentVersionRepository documentVersionRepository;

    public DocumentRecordService(DocumentRecordRepository documentRecordRepository,
                                DocumentVersionRepository documentVersionRepository) {
        this.documentRecordRepository = documentRecordRepository;
        this.documentVersionRepository = documentVersionRepository;
    }

    @Transactional
    public DocumentRecord storeDocument(String documentType, String documentName, String description,
                                      String storageLocation, String associatedModule, String eccnClassification,
                                      String createdBy) {
        DocumentRecord record = new DocumentRecord(documentType, documentName, description,
                storageLocation, associatedModule, eccnClassification, createdBy);
        return documentRecordRepository.save(record);
    }

    @Transactional
    public DocumentVersion createDocumentVersion(UUID documentId, String newContent) {
        DocumentRecord document = documentRecordRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        DocumentVersion version = new DocumentVersion(document, newContent);
        return documentVersionRepository.save(version);
    }

    public List<DocumentVersion> getDocumentVersions(UUID documentId) {
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId.toString());
    }

    public String compareDocumentVersions(UUID documentId, int version1, int version2) {
        DocumentVersion v1 = documentVersionRepository.findByDocumentIdAndVersionNumber(documentId.toString(), version1)
                .orElseThrow(() -> new IllegalArgumentException("Version 1 not found"));
        DocumentVersion v2 = documentVersionRepository.findByDocumentIdAndVersionNumber(documentId.toString(), version2)
                .orElseThrow(() -> new IllegalArgumentException("Version 2 not found"));
        return generateDiff(v1.getContent(), v2.getContent());
    }

    public List<DocumentRecord> getDocumentsByTypeAndModule(String documentType, String moduleName) {
        return documentRecordRepository.findByDocumentTypeAndAssociatedModule(documentType, moduleName);
    }

    public DocumentRecord generateDocument(String templateName, Map<String, String> data) {
        return storeDocument("Generated", templateName, "Auto-generated document",
                "System", "Templates", "N/A", "System");
    }

    @Transactional
    public void linkDocuments(UUID documentId1, UUID documentId2, String relationshipType) {
        DocumentRecord doc1 = documentRecordRepository.findById(documentId1)
                .orElseThrow(() -> new IllegalArgumentException("Document 1 not found"));
        DocumentRecord doc2 = documentRecordRepository.findById(documentId2)
                .orElseThrow(() -> new IllegalArgumentException("Document 2 not found"));
        doc1.addRelatedDocument(doc2, relationshipType);
        documentRecordRepository.save(doc1);
    }

    public List<DocumentRecord> getDocumentsByType(String documentType) {
        return documentRecordRepository.findByDocumentType(documentType);
    }

    public List<DocumentRecord> searchDocuments(String searchTerm) {
        return documentRecordRepository.findByDocumentNameContainingIgnoreCase(searchTerm);
    }

    public List<DocumentRecord> getDocumentsByModule(String moduleName) {
        return documentRecordRepository.findByAssociatedModule(moduleName);
    }

    public List<DocumentRecord> getDocumentsByEccn(String eccnClassification) {
        return documentRecordRepository.findByEccnClassification(eccnClassification);
    }

    @Transactional
    public void archiveExpiredDocuments() {
        LocalDate now = LocalDate.now();
        List<DocumentRecord> expiredDocuments = documentRecordRepository.findByExpirationDateBefore(now);
        expiredDocuments.forEach(doc -> doc.setArchived(true));
        documentRecordRepository.saveAll(expiredDocuments);
    }

    public List<DocumentRecord> getAuditTrail(String username) {
        return documentRecordRepository.findByCreatedBy(username);
    }

    @Transactional
    public void deleteArchivedDocuments() {
        List<DocumentRecord> archivedDocuments = documentRecordRepository.findByArchived(true);
        documentRecordRepository.deleteAll(archivedDocuments);
    }

    public List<DocumentRecord> getDocumentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return documentRecordRepository.findByCreationDateBetween(startDate, endDate);
    }

    private String generateDiff(String content1, String content2) {
        List<String> original = Arrays.asList(content1.split("\n"));
        List<String> revised = Arrays.asList(content2.split("\n"));
        
        // Simple line-by-line diff implementation
        StringBuilder diff = new StringBuilder();
        int maxLines = Math.max(original.size(), revised.size());
        
        for (int i = 0; i < maxLines; i++) {
            String originalLine = i < original.size() ? original.get(i) : "";
            String revisedLine = i < revised.size() ? revised.get(i) : "";
            
            if (!originalLine.equals(revisedLine)) {
                diff.append("- ").append(originalLine).append("\n");
                diff.append("+ ").append(revisedLine).append("\n");
            } else {
                diff.append("  ").append(originalLine).append("\n");
            }
        }
        
        return diff.toString();
    }
}
