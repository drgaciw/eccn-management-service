package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.DocumentRecord;
import com.aciworldwide.eccn_management_service.model.DocumentVersion;
import com.aciworldwide.eccn_management_service.repository.DocumentRecordRepository;
import com.aciworldwide.eccn_management_service.repository.DocumentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentRecordServiceTest {

    @Mock
    private DocumentRecordRepository documentRecordRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @InjectMocks
    private DocumentRecordService documentRecordService;

    private DocumentRecord testDocument;
    private UUID testDocumentId;

    @BeforeEach
    void setUp() {
        testDocumentId = UUID.randomUUID();
        testDocument = new DocumentRecord("TECHNICAL_SPECS", "Test Document", "Test Description",
                "/path/to/doc", "Module1", "ECCN1", "testuser");
        testDocument.setId(testDocumentId);
    }

    @Test
    void testCreateDocumentVersion() {
        String newContent = "Updated content";
        DocumentVersion version = new DocumentVersion(testDocument, newContent);
        
        when(documentRecordRepository.findById(testDocumentId)).thenReturn(Optional.of(testDocument));
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(version);

        DocumentVersion result = documentRecordService.createDocumentVersion(testDocumentId, newContent);
        
        assertNotNull(result);
        assertEquals(newContent, result.getContent());
        assertEquals(1, result.getVersionNumber());
        verify(documentVersionRepository).save(any(DocumentVersion.class));
    }

    @Test
    void testGetDocumentVersions() {
        DocumentVersion version1 = new DocumentVersion(testDocument, "Content 1");
        DocumentVersion version2 = new DocumentVersion(testDocument, "Content 2");
        List<DocumentVersion> versions = Arrays.asList(version1, version2);
        
        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(testDocumentId.toString()))
                .thenReturn(versions);

        List<DocumentVersion> result = documentRecordService.getDocumentVersions(testDocumentId);
        
        assertEquals(2, result.size());
        assertEquals("Content 1", result.get(0).getContent());
    }

    @Test
    void testCompareDocumentVersions() {
        DocumentVersion version1 = new DocumentVersion(testDocument, "Content 1");
        DocumentVersion version2 = new DocumentVersion(testDocument, "Content 2");
        
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(testDocumentId.toString(), 1))
                .thenReturn(Optional.of(version1));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(testDocumentId.toString(), 2))
                .thenReturn(Optional.of(version2));

        String diff = documentRecordService.compareDocumentVersions(testDocumentId, 1, 2);
        
        assertNotNull(diff);
        assertTrue(diff.contains("- Content 1"));
        assertTrue(diff.contains("+ Content 2"));
    }

    @Test
    void testLinkDocuments() {
        DocumentRecord relatedDoc = new DocumentRecord("CLASSIFICATION_REPORT", "Related Doc", "Description",
                "/path/to/related", "Module1", "ECCN1", "testuser");
        UUID relatedDocId = UUID.randomUUID();
        relatedDoc.setId(relatedDocId);
        
        when(documentRecordRepository.findById(testDocumentId)).thenReturn(Optional.of(testDocument));
        when(documentRecordRepository.findById(relatedDocId)).thenReturn(Optional.of(relatedDoc));

        documentRecordService.linkDocuments(testDocumentId, relatedDocId, "RELATED_TO");
        
        assertTrue(testDocument.getRelatedDocuments().containsKey(relatedDoc));
        verify(documentRecordRepository).save(testDocument);
    }

    @Test
    void testGenerateDocument() {
        Map<String, String> data = Map.of("field1", "value1", "field2", "value2");
        
        when(documentRecordRepository.save(any(DocumentRecord.class))).thenReturn(testDocument);

        DocumentRecord result = documentRecordService.generateDocument("TestTemplate", data);
        
        assertNotNull(result);
        assertEquals("TECHNICAL_SPECS", result.getDocumentType());
        assertEquals("Test Document", result.getDocumentName());
    }

    @Test
    void testGetDocumentsByTypeAndModule() {
        List<DocumentRecord> documents = Collections.singletonList(testDocument);
        
        when(documentRecordRepository.findByDocumentTypeAndAssociatedModule("TECHNICAL_SPECS", "Module1"))
                .thenReturn(documents);

        List<DocumentRecord> result = documentRecordService.getDocumentsByTypeAndModule("TECHNICAL_SPECS", "Module1");
        
        assertEquals(1, result.size());
        assertEquals(testDocumentId, result.get(0).getId());
    }
}