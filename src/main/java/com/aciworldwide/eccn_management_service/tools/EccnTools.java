package com.aciworldwide.eccn_management_service.tools;

import com.aciworldwide.eccn_management_service.model.DocumentRecord;
import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.model.ExportControl;
import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import com.aciworldwide.eccn_management_service.model.Product;
import com.aciworldwide.eccn_management_service.model.RiskAssessment;
import com.aciworldwide.eccn_management_service.service.AutomatedClassificationToolService;
import com.aciworldwide.eccn_management_service.service.CryptoClassificationService;
import com.aciworldwide.eccn_management_service.service.DocumentRecordService;
import com.aciworldwide.eccn_management_service.service.EccnClassificationWorkflowService;
import com.aciworldwide.eccn_management_service.service.EccnService;
import com.aciworldwide.eccn_management_service.service.ExportControlService;
import com.aciworldwide.eccn_management_service.service.GlossaryService;
import com.aciworldwide.eccn_management_service.service.ProductService;
import com.aciworldwide.eccn_management_service.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EccnTools {

    private static final Logger logger = LoggerFactory.getLogger(EccnTools.class);

    private final EccnService eccnService;
    private final ProductService productService;
    private final CryptoClassificationService cryptoClassificationService;
    private final GlossaryService glossaryService;
    private final DocumentRecordService documentRecordService;
    private final RiskAssessmentService riskAssessmentService;
    private final ExportControlService exportControlService;
    private final EccnClassificationWorkflowService workflowService;
    private final AutomatedClassificationToolService automatedClassificationToolService;

    public EccnTools(EccnService eccnService,
                     ProductService productService,
                     CryptoClassificationService cryptoClassificationService,
                     GlossaryService glossaryService,
                     DocumentRecordService documentRecordService,
                     RiskAssessmentService riskAssessmentService,
                     ExportControlService exportControlService,
                     EccnClassificationWorkflowService workflowService,
                     AutomatedClassificationToolService automatedClassificationToolService) {
        this.eccnService = eccnService;
        this.productService = productService;
        this.cryptoClassificationService = cryptoClassificationService;
        this.glossaryService = glossaryService;
        this.documentRecordService = documentRecordService;
        this.riskAssessmentService = riskAssessmentService;
        this.exportControlService = exportControlService;
        this.workflowService = workflowService;
        this.automatedClassificationToolService = automatedClassificationToolService;
    }

    @Tool(description = "Search ECCN records by code or description. Returns matching ECCN classifications.")
    public String searchEccnRecords(String query) {
        logger.debug("Tool: searchEccnRecords({})", query);
        try {
            List<Eccn> results = eccnService.searchEccns(query);
            if (results.isEmpty()) {
                return "No ECCN records found matching: " + query;
            }
            return results.stream()
                .map(e -> String.format("ECCN %s: %s (Category: %s, Controls: %s)",
                    e.getCommodityCode(), e.getDescription(),
                    e.getCategory(), e.getControlReasons()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("searchEccnRecords failed: {}", e.getMessage());
            return "Error searching ECCN records. An internal error occurred.";
        }
    }

    @Tool(description = "Get detailed information for a specific ECCN by its commodity code (e.g., 5A002).")
    public String getEccnRecord(String eccnCode) {
        logger.debug("Tool: getEccnRecord({})", eccnCode);
        try {
            Eccn eccn = eccnService.findById(eccnCode);
            return String.format("ECCN %s\nDescription: %s\nCategory: %s\nSubCategory: %s\n"
                + "Control Reasons: %s\nFinancial Software: %s\nData Analytics: %s\nDeprecated: %s",
                eccn.getCommodityCode(), eccn.getDescription(),
                eccn.getCategory(), eccn.getSubCategory(),
                eccn.getControlReasons(), eccn.isFinancialSoftware(),
                eccn.isDataAnalytics(), eccn.isDeprecated());
        } catch (IllegalArgumentException e) {
            return "No ECCN record found matching code: " + eccnCode;
        } catch (Exception e) {
            logger.error("getEccnRecord failed: {}", e.getMessage());
            return "Error retrieving ECCN record. An internal error occurred.";
        }
    }

    @Tool(description = "Search products by name. Returns matching products with their classification status.")
    public String searchProducts(String name) {
        logger.debug("Tool: searchProducts({})", name);
        try {
            List<Product> results = productService.searchProductsByName(name);
            if (results.isEmpty()) {
                return "No products found matching: " + name;
            }
            return results.stream()
                .map(p -> String.format("Product: %s (Status: %s, Versions: %d)",
                    p.getName(), p.getStatus(), p.getVersions().size()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("searchProducts failed: {}", e.getMessage());
            return "Error searching products. An internal error occurred.";
        }
    }

    @Tool(description = "Get products by their classification status (e.g., ACTIVE, PENDING, CLASSIFIED).")
    public String getProductsByStatus(String status) {
        logger.debug("Tool: getProductsByStatus({})", status);
        try {
            List<Product> results = productService.getProductsByStatus(status);
            if (results.isEmpty()) {
                return "No products found with status: " + status;
            }
            return results.stream()
                .map(p -> String.format("Product: %s (Status: %s, Versions: %d)",
                    p.getName(), p.getStatus(), p.getVersions().size()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("getProductsByStatus failed: {}", e.getMessage());
            return "Error retrieving products by status. An internal error occurred.";
        }
    }

    @Tool(description = "Classify cryptographic product based on key length and algorithm. "
        + "Algorithm must be one of: AES, RSA, ECC, BLOWFISH, SHA3. Returns ECCN classification string.")
    public String classifyCrypto(int keyLength, String algorithm) {
        logger.debug("Tool: classifyCrypto({}, {})", keyLength, algorithm);
        if (algorithm == null || algorithm.isBlank()) {
            return "Invalid algorithm: algorithm parameter must not be null or blank. Must be one of: AES, RSA, ECC, BLOWFISH, SHA3";
        }
        try {
            CryptoClassificationService.Algorithm algo =
                CryptoClassificationService.Algorithm.valueOf(algorithm.toUpperCase());
            String classification = cryptoClassificationService.classifyCryptography(keyLength, algo, false);
            return String.format("Crypto classification for %s (key length %d): %s", algorithm, keyLength, classification);
        } catch (IllegalArgumentException e) {
            return "Invalid algorithm: " + algorithm + ". Must be one of: AES, RSA, ECC, BLOWFISH, SHA3";
        } catch (Exception e) {
            logger.error("classifyCrypto failed: {}", e.getMessage());
            return "Error classifying cryptography. An internal error occurred.";
        }
    }

    @Tool(description = "Search the ECCN glossary for terms matching the given text. Returns definitions and categories.")
    public String searchGlossary(String term) {
        logger.debug("Tool: searchGlossary({})", term);
        try {
            List<GlossaryEntry> results = glossaryService.searchByTermPart(term);
            if (results.isEmpty()) {
                return "No glossary entries found matching: " + term;
            }
            return results.stream()
                .map(g -> String.format("%s [%s]: %s", g.getTerm(), g.getCategory(), g.getDefinition()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("searchGlossary failed: {}", e.getMessage());
            return "Error searching glossary. An internal error occurred.";
        }
    }

    @Tool(description = "Search compliance documents by name. Returns matching document records with metadata.")
    public String searchDocuments(String query) {
        logger.debug("Tool: searchDocuments({})", query);
        try {
            List<DocumentRecord> results = documentRecordService.searchDocuments(query);
            if (results.isEmpty()) {
                return "No documents found matching: " + query;
            }
            return results.stream()
                .map(d -> String.format("Document: %s (Type: %s, Module: %s, ECCN: %s, Created: %s)",
                    d.getDocumentName(), d.getDocumentType(),
                    d.getAssociatedModule(), d.getEccnClassification(),
                    d.getCreationDate()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("searchDocuments failed: {}", e.getMessage());
            return "Error searching documents. An internal error occurred.";
        }
    }

    @Tool(description = "Get risk assessments for a specific module. Returns risk level, restricted end uses, and mitigation actions.")
    public String getRiskAssessments(String moduleName) {
        logger.debug("Tool: getRiskAssessments({})", moduleName);
        try {
            List<RiskAssessment> results = riskAssessmentService.getAssessmentsByModule(moduleName);
            if (results.isEmpty()) {
                return "No risk assessments found for module: " + moduleName;
            }
            return results.stream()
                .map(r -> String.format("Risk Assessment for %s: Level=%s, End Uses=%s, "
                    + "High Risk Users=%s, Mitigations=%s, Follow-up=%s",
                    r.getModuleName(), r.getRiskLevel(), r.getRestrictedEndUses(),
                    r.getHighRiskUsers(), r.getMitigationActions(), r.isRequiresFollowUp()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("getRiskAssessments failed: {}", e.getMessage());
            return "Error retrieving risk assessments. An internal error occurred.";
        }
    }

    @Tool(description = "Get all high-risk assessments across all modules. Returns assessments with HIGH risk level.")
    public String getHighRiskAssessments() {
        logger.debug("Tool: getHighRiskAssessments()");
        try {
            List<RiskAssessment> results = riskAssessmentService.getHighRiskAssessments();
            if (results.isEmpty()) {
                return "No high-risk assessments found.";
            }
            return results.stream()
                .map(r -> String.format("HIGH RISK: %s — End Uses: %s, Users: %s, Mitigations: %s",
                    r.getModuleName(), r.getRestrictedEndUses(),
                    r.getHighRiskUsers(), r.getMitigationActions()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("getHighRiskAssessments failed: {}", e.getMessage());
            return "Error retrieving high-risk assessments. An internal error occurred.";
        }
    }

    @Tool(description = "Get export control classifications for a specific module. Returns EAR classification and jurisdiction details.")
    public String getExportControls(String moduleName) {
        logger.debug("Tool: getExportControls({})", moduleName);
        try {
            List<ExportControl> results = exportControlService.getExportControlsByModule(moduleName);
            if (results.isEmpty()) {
                return "No export controls found for module: " + moduleName;
            }
            return results.stream()
                .map(ec -> String.format("Export Control for %s: EAR=%s, Unified=%s, "
                    + "Jurisdictions=%s, Special Handling=%s, Requirements=%s",
                    ec.getModuleName(), ec.getEarClassification(),
                    ec.getUnifiedClassification(), ec.getJurisdictionClassifications(),
                    ec.isRequiresSpecialHandling(), ec.getComplianceRequirements()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("getExportControls failed: {}", e.getMessage());
            return "Error retrieving export controls. An internal error occurred.";
        }
    }

    @Tool(description = "Get the classification workflow status for a release version. "
        + "Returns the current workflow state for the given release.")
    public String getWorkflowStatus(String releaseVersion) {
        logger.debug("Tool: getWorkflowStatus({})", releaseVersion);
        return "Classification workflow status for release '" + releaseVersion + "': "
            + "The workflow service operates as an in-memory state machine. "
            + "Active requests are created via the REST API and exist only during the current session. "
            + "To check workflow status, use the classification workflow REST endpoints directly.";
    }

    @Tool(description = "Suggest ECCN classifications for a module based on its encryption libraries "
        + "and payment processing capabilities. Returns classification with confidence scores.")
    public String suggestEccn(String moduleName) {
        logger.debug("Tool: suggestEccn({})", moduleName);
        try {
            Map<String, Double> suggestions = automatedClassificationToolService.suggestECCN(moduleName);
            if (suggestions.isEmpty()) {
                return "No classification suggestions available for module: " + moduleName
                    + ". The module may not have been analyzed yet, the AI model may be unavailable, "
                    + "or the AI response could not be interpreted. "
                    + "Run analyzeSourceCode() or analyzeSoftwarePackage() first, "
                    + "and verify that the AI classification provider is configured.";
            }
            return "ECCN suggestions for " + moduleName + ":\n"
                + suggestions.entrySet().stream()
                    .map(e -> String.format("  %s: %.1f%% confidence", e.getKey(), e.getValue() * 100))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.error("suggestEccn failed: {}", e.getMessage());
            return "Error suggesting ECCN classification. An internal error occurred.";
        }
    }

    @Tool(description = "Analyze source code repository for encryption libraries and payment capabilities to build classification input.")
    public String analyzeSourceCode(String repositoryUrl, String branch, String commit) {
        logger.debug("Tool: analyzeSourceCode({}, {}, {})", repositoryUrl, branch, commit);
        try {
            AutomatedClassificationToolService.ModuleAnalysis analysis = 
                automatedClassificationToolService.analyzeSourceCode(repositoryUrl, branch, commit);
            return String.format("Source code analysis complete for %s:%s:%s. Found encryption libraries: %s. Payment processing: %s.",
                repositoryUrl, branch, commit, analysis.getEncryptionLibraries(), analysis.isHasPaymentProcessing());
        } catch (Exception e) {
            logger.error("analyzeSourceCode failed: {}", e.getMessage());
            return "Error analyzing source code for repository: " + repositoryUrl;
        }
    }

    @Tool(description = "Analyze a software package artifact for cryptographic algorithms and export control triggers.")
    public String analyzeSoftwarePackage(String packageName, String packageType) {
        logger.debug("Tool: analyzeSoftwarePackage({}, {})", packageName, packageType);
        try {
            AutomatedClassificationToolService.ModuleAnalysis analysis = 
                automatedClassificationToolService.analyzeSoftwarePackage(packageName, packageType);
            return String.format("Software package analysis complete for %s (%s). Found encryption libraries: %s. Payment processing: %s.",
                packageName, packageType, analysis.getEncryptionLibraries(), analysis.isHasPaymentProcessing());
        } catch (Exception e) {
            logger.error("analyzeSoftwarePackage failed: {}", e.getMessage());
            return "Error analyzing software package: " + packageName;
        }
    }

    @Tool(description = "Validate an ECCN code against external databases. NOT YET IMPLEMENTED — "
        + "external integration services are pending.")
    public String validateExternalEccn(String eccnCode) {
        logger.debug("Tool: validateExternalEccn({}) — no-op stub", eccnCode);
        return "External ECCN validation is not yet available. "
            + "Use standard ECCN format validation instead. "
            + "The ECCN code '" + eccnCode + "' has not been validated against external databases.";
    }

    @Tool(description = "Get integration status for an external system. NOT YET IMPLEMENTED — "
        + "enterprise integration services are pending.")
    public String getIntegrationStatus(String system) {
        logger.debug("Tool: getIntegrationStatus({}) — no-op stub", system);
        return "Integration status check is not yet available. "
            + "The enterprise integration module for system '" + system + "' is pending implementation.";
    }
}
