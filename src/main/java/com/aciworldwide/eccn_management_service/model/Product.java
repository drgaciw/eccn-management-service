package com.aciworldwide.eccn_management_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private String description;
    private List<String> features;
    private List<VersionRelease> versions;
    private String status; // ACTIVE, DEPRECATED, etc.
    private double usContentPercentage;
    private boolean militaryUse;
    private boolean controlledCountry;
    private boolean encryptionEnabled;

    @Data
    public static class VersionRelease {
        private String versionNumber;
        private String releaseDate;
        private List<String> encryptionLibraries;
        private String classificationStatus; // PENDING, CLASSIFIED
    }

    public double getUsContentPercentage() {
        return usContentPercentage;
    }

    public void setUsContentPercentage(double usContentPercentage) {
        this.usContentPercentage = usContentPercentage;
    }

    public boolean isMilitaryUse() {
        return militaryUse;
    }

    public void setMilitaryUse(boolean militaryUse) {
        this.militaryUse = militaryUse;
    }

    public boolean isControlledCountry() {
        return controlledCountry;
    }

    public void setControlledCountry(boolean controlledCountry) {
        this.controlledCountry = controlledCountry;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }
}