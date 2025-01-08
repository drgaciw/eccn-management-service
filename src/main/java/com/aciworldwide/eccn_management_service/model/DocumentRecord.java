package com.aciworldwide.eccn_management_service.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "document_records")
@Data
@NoArgsConstructor
public class DocumentRecord {
    @Id
    private UUID id;
    private String documentType; // e.g., TECHNICAL_SPECS, CLASSIFICATION_JUSTIFICATION
    private String documentName;
    private String description;
    private String storageLocation; // Path or reference to actual document
    private String associatedModule; // Related software module
    private String eccnClassification; // Related ECCN classification
    private LocalDate creationDate;
    private LocalDate expirationDate;
    private String createdBy;
    private boolean archived = false;
    
    @DBRef
    private List<DocumentVersion> versions = new ArrayList<>();
    
    @DBRef
    private Map<DocumentRecord, String> relatedDocuments = new HashMap<>();

    public DocumentRecord(String documentType, String documentName, String description,
                         String storageLocation, String associatedModule, String eccnClassification,
                         String createdBy) {
        this.id = UUID.randomUUID();
        this.documentType = documentType;
        this.documentName = documentName;
        this.description = description;
        this.storageLocation = storageLocation;
        this.associatedModule = associatedModule;
        this.eccnClassification = eccnClassification;
        this.creationDate = LocalDate.now();
        this.expirationDate = this.creationDate.plusYears(5); // 5 year retention
        this.createdBy = createdBy;
    }

    public void addVersion(DocumentVersion version) {
        this.versions.add(version);
    }

    public void addRelatedDocument(DocumentRecord relatedDocument, String relationshipType) {
        this.relatedDocuments.put(relatedDocument, relationshipType);
    }

    public List<DocumentVersion> getVersions() {
        return new ArrayList<>(this.versions);
    }
}