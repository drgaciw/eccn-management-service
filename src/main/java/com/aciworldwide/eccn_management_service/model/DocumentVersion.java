package com.aciworldwide.eccn_management_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document
public class DocumentVersion {
    @Id
    private String id;
    
    @DBRef
    @Field("document_id")
    private DocumentRecord document;
    
    @Field
    private String content;
    
    @Field
    private int versionNumber;
    
    @Field
    private LocalDateTime createdDate;
    
    @Field
    private String createdBy;

    public DocumentVersion(DocumentRecord document, String content) {
        this.document = document;
        this.content = content;
        this.versionNumber = document.getVersions().size() + 1;
        this.createdBy = "System"; // Default, can be overridden
    }
}