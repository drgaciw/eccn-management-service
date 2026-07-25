package com.aciworldwide.eccn_management_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "glossary_entries")
public class GlossaryEntry {
    @Id
    private String id;

    @Indexed(unique = true)
    private String term;
    private String definition;
    private String regulatoryContext;
    private List<String> crossReferences;
    private String technicalDetails;
    private List<String> specialConditions;
    @Indexed
    private String category;
    private Map<String, String> additionalNotes;
    private String source;
    private String lastUpdated;

    // Default constructor
    public GlossaryEntry() {}

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getRegulatoryContext() {
        return regulatoryContext;
    }

    public void setRegulatoryContext(String regulatoryContext) {
        this.regulatoryContext = regulatoryContext;
    }

    public List<String> getCrossReferences() {
        return crossReferences;
    }

    public void setCrossReferences(List<String> crossReferences) {
        this.crossReferences = crossReferences;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    public void setTechnicalDetails(String technicalDetails) {
        this.technicalDetails = technicalDetails;
    }

    public List<String> getSpecialConditions() {
        return specialConditions;
    }

    public void setSpecialConditions(List<String> specialConditions) {
        this.specialConditions = specialConditions;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, String> getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(Map<String, String> additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}