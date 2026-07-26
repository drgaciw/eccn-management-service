package com.aciworldwide.eccn_management_service.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "eccns")
@CompoundIndexes({
    @CompoundIndex(name = "eccn_category_subcategory_idx", def = "{'category': 1, 'subCategory': 1}")
})
@Schema(description = "Export Control Classification Number")
public class Eccn {
    @Id
    @Schema(description = "Unique identifier", example = "507f1f77bcf86cd799439011")
    private String id;

    @Schema(description = "ECCN classification code", example = "5D002", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "Detailed description of the classification", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Schema(description = "Main category of the classification", example = "Software", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @Schema(description = "Sub-category of the classification", example = "Information Security")
    private String subCategory;

    @Schema(description = "Reason for control", example = "NS1, AT1")
    private String controlReason;

    @Schema(description = "Indicates if a license is required", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean licenseRequired;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Flag indicating if this ECCN is deprecated")
    private boolean deprecated;
    
    @Schema(description = "Reason for deprecation")
    private String deprecationReason;
    
    @Schema(description = "ID of the ECCN that replaces this one if deprecated")
    private String replacementEccnId;
    
    @Schema(description = "List of related ECCNs")
    private List<String> relatedEccns;
    
    @Indexed(unique = true)
    @Schema(description = "Commodity code")
    @JsonAlias({"eccnCode", "eccn_code"})
    private String commodityCode;
    
    @Schema(description = "Flag indicating if this ECCN is encryption related")
    private boolean encryptionRelated;

    @Schema(description = "Flag indicating if this ECCN applies to financial software")
    private boolean financialSoftware;
    
    @Schema(description = "Flag indicating if this ECCN applies to data analytics")
    private boolean dataAnalytics;
    
    @Schema(description = "List of analytics capabilities if applicable")
    private List<String> analyticsCapabilities;
    
    @Schema(description = "List of applicable EAR controls")
    private List<String> applicableEARControls;
    
    @Schema(description = "List of control reasons")
    private List<String> controlReasons;
    
    /**
     * Nested class to represent ECCN history entries
     */
    @Data
    public static class EccnHistoryEntry {
        @Schema(description = "Timestamp of the history entry")
        private LocalDateTime timestamp;
        
        @Schema(description = "Type of change")
        private String changeType;
        
        @Schema(description = "User who made the change")
        private String changedBy;
        
        @Schema(description = "Previous values before the change")
        private Eccn previousValues;
        
        @Schema(description = "Comments about the change")
        private String comments;
    }
}
