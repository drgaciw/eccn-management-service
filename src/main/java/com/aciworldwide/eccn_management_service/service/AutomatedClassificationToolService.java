package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.repository.ClassificationHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AutomatedClassificationToolService {
    private static final Logger logger = LoggerFactory.getLogger(AutomatedClassificationToolService.class);

    private final ClassificationHistoryRepository classificationHistoryRepository;
    private final CryptoClassificationService cryptoClassificationService;
    private AIModel aiModel;

    public AutomatedClassificationToolService(
            ClassificationHistoryRepository classificationHistoryRepository,
            CryptoClassificationService cryptoClassificationService) {
        this.classificationHistoryRepository = Objects.requireNonNull(classificationHistoryRepository, "ClassificationHistoryRepository must not be null");
        this.cryptoClassificationService = Objects.requireNonNull(cryptoClassificationService, "CryptoClassificationService must not be null");
    }

    public static class ModuleAnalysis {
        private String moduleName;
        private ModuleType moduleType;
        private List<String> encryptionLibraries;
        private boolean hasPaymentProcessing;
        private String eccnClassification;
        private LocalDateTime analysisTimestamp;
        private String classificationRationale;

        public enum ModuleType {
            SOURCE_CODE, SOFTWARE_PACKAGE
        }

        // Getters and setters
        public String getModuleName() { return moduleName; }
        public void setModuleName(String moduleName) { this.moduleName = moduleName; }
        public ModuleType getModuleType() { return moduleType; }
        public void setModuleType(ModuleType moduleType) { this.moduleType = moduleType; }
        public List<String> getEncryptionLibraries() { return encryptionLibraries; }
        public void setEncryptionLibraries(List<String> encryptionLibraries) { this.encryptionLibraries = encryptionLibraries; }
        public boolean isHasPaymentProcessing() { return hasPaymentProcessing; }
        public void setHasPaymentProcessing(boolean hasPaymentProcessing) { this.hasPaymentProcessing = hasPaymentProcessing; }
        public String getEccnClassification() { return eccnClassification; }
        public void setEccnClassification(String eccnClassification) { this.eccnClassification = eccnClassification; }
        public LocalDateTime getAnalysisTimestamp() { return analysisTimestamp; }
        public void setAnalysisTimestamp(LocalDateTime analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }
        public String getClassificationRationale() { return classificationRationale; }
        public void setClassificationRationale(String classificationRationale) { this.classificationRationale = classificationRationale; }
    }

    /**
     * Analyzes source code for cryptographic libraries and payment processing capabilities.
     * @param repositoryUrl URL of the source code repository
     * @param branch Branch to analyze
     * @param commit Commit hash to analyze
     * @return ModuleAnalysis containing the analysis results
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public ModuleAnalysis analyzeSourceCode(String repositoryUrl, String branch, String commit) {
        validateInputParameters(repositoryUrl, branch, commit);
        logger.info("Analyzing source code: {}:{}:{}", repositoryUrl, branch, commit);

        ModuleAnalysis analysis = new ModuleAnalysis();
        analysis.setModuleName(repositoryUrl + ":" + branch + ":" + commit);
        analysis.setModuleType(ModuleAnalysis.ModuleType.SOURCE_CODE);
        
        Map<String, Object> analysisResult = analyzeSourceCodeContent(repositoryUrl, branch, commit);
        processAnalysisResult(analysis, analysisResult);
        
        return performClassification(analysis);
    }

    /**
     * Analyzes a software package for cryptographic libraries and payment processing capabilities.
     * @param packageName Name of the software package
     * @param packageType Type of the software package
     * @return ModuleAnalysis containing the analysis results
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public ModuleAnalysis analyzeSoftwarePackage(String packageName, String packageType) {
        validateInputParameters(packageName, packageType);
        logger.info("Analyzing software package: {}:{}", packageName, packageType);

        ModuleAnalysis analysis = new ModuleAnalysis();
        analysis.setModuleName(packageName);
        analysis.setModuleType(ModuleAnalysis.ModuleType.SOFTWARE_PACKAGE);
        
        Map<String, Object> analysisResult = analyzePackageContent(packageName, packageType);
        processAnalysisResult(analysis, analysisResult);
        
        return performClassification(analysis);
    }

    private void validateInputParameters(String... parameters) {
        for (String param : parameters) {
            if (param == null || param.trim().isEmpty()) {
                throw new IllegalArgumentException("Parameters cannot be null or empty");
            }
        }
    }

    private void processAnalysisResult(ModuleAnalysis analysis, Map<String, Object> analysisResult) {
        @SuppressWarnings("unchecked")
        List<String> encryptionLibraries = (List<String>) analysisResult.get("encryptionLibraries");
        analysis.setEncryptionLibraries(encryptionLibraries);
        analysis.setHasPaymentProcessing((boolean) analysisResult.get("hasPaymentProcessing"));
    }

    /**
     * Validates a proposed ECCN classification against the module's analysis.
     * @param moduleName Name of the module to validate
     * @param proposedECCN Proposed ECCN classification
     * @return true if the proposed classification matches the analysis, false otherwise
     */
    public boolean validateClassification(String moduleName, String proposedECCN) {
        Objects.requireNonNull(moduleName, "Module name cannot be null");
        Objects.requireNonNull(proposedECCN, "Proposed ECCN cannot be null");

        ModuleAnalysis analysis = getLatestAnalysis(moduleName);
        if (analysis == null) {
            logger.warn("No analysis found for module: {}", moduleName);
            return false;
        }
        
        // Get the classification and ensure it's not null
        String classification = determineClassification(analysis.getEncryptionLibraries(), analysis.isHasPaymentProcessing());
        if (classification == null) {
            logger.warn("Could not determine classification for module: {}", moduleName);
            return false;
        }
        
        // Compare the proposed ECCN with the determined classification
        return classification.equals(proposedECCN);
    }

    /**
     * Suggests possible ECCN classifications for a module.
     * @param moduleName Name of the module to suggest classifications for
     * @return Map of suggested ECCN classifications with confidence scores
     */
    public Map<String, Double> suggestECCN(String moduleName) {
        Objects.requireNonNull(moduleName, "Module name cannot be null");

        ModuleAnalysis analysis = getLatestAnalysis(moduleName);
        if (analysis == null || aiModel == null) {
            logger.warn("No analysis or AI model available for module: {}", moduleName);
            return Map.of();
        }
        return aiModel.suggestClassifications(
                analysis.getEncryptionLibraries(),
                analysis.isHasPaymentProcessing()
        );
    }

    /**
     * Generates a detailed classification report for a module.
     * @param moduleName Name of the module to generate the report for
     * @return String containing the classification report
     */
    public String generateClassificationReport(String moduleName) {
        Objects.requireNonNull(moduleName, "Module name cannot be null");

        ModuleAnalysis analysis = getLatestAnalysis(moduleName);
        if (analysis == null) {
            logger.warn("No analysis found for module: {}", moduleName);
            return "No analysis found for module: " + moduleName;
        }
        return String.format("Classification Report for %s:\n" +
                        "ECCN: %s\n" +
                        "Rationale: %s\n" +
                        "Analysis Timestamp: %s",
                moduleName,
                analysis.getEccnClassification(),
                analysis.getClassificationRationale(),
                analysis.getAnalysisTimestamp());
    }

    public void integrateAIModel(AIModel model) {
        this.aiModel = Objects.requireNonNull(model, "AI model cannot be null");
    }

    private ModuleAnalysis performClassification(ModuleAnalysis analysis) {
        analysis.setAnalysisTimestamp(LocalDateTime.now());
        String classification = determineClassification(
                analysis.getEncryptionLibraries(),
                analysis.isHasPaymentProcessing()
        );
        analysis.setEccnClassification(classification);
        analysis.setClassificationRationale(generateClassificationRationale(analysis));
        
        classificationHistoryRepository.save(analysis);
        return analysis;
    }

    /**
     * Determines the ECCN classification based on encryption libraries and payment processing.
     * @param encryptionLibraries List of encryption libraries used
     * @param hasPaymentProcessing Whether the module has payment processing capabilities
     * @return String representing the ECCN classification
     */
    public String determineClassification(List<String> encryptionLibraries, boolean hasPaymentProcessing) {
        if (encryptionLibraries == null || encryptionLibraries.isEmpty()) {
            return hasPaymentProcessing ? "5A002" : "EAR99";
        }

        // If AI model is available, use it for classification
        if (aiModel != null) {
            String aiClassification = aiModel.classify(encryptionLibraries, hasPaymentProcessing);
            if (aiClassification != null) {
                return aiClassification;
            }
        }

        // Default classification logic
        for (String library : encryptionLibraries) {
            String tempResult = cryptoClassificationService.classifyCryptography(
                    getLibraryKeyLength(library),
                    getAlgorithmForLibrary(library),
                    false
            );
            if (tempResult != null) {
                return tempResult; // Return the classification from the crypto service
            }
        }

        // If no specific classification found, return EAR99 or 5A002 based on payment processing
        return hasPaymentProcessing ? "5A002" : "EAR99";
    }

    private String generateClassificationRationale(ModuleAnalysis analysis) {
        StringBuilder rationale = new StringBuilder();
        rationale.append("Module type: ").append(analysis.getModuleType()).append("\n");
        rationale.append("Encryption libraries: ").append(String.join(", ", analysis.getEncryptionLibraries())).append("\n");
        rationale.append("Payment processing: ").append(analysis.isHasPaymentProcessing()).append("\n");
        return rationale.toString();
    }

    private ModuleAnalysis getLatestAnalysis(String moduleName) {
        List<ModuleAnalysis> history = getClassificationHistory(moduleName);
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    public int getLibraryKeyLength(String library) {
        return cryptoClassificationService.getLibraryKeyLength(library, "latest");
    }

    public CryptoClassificationService.Algorithm getAlgorithmForLibrary(String library) {
        return cryptoClassificationService.getLibraryAlgorithm(library, "latest");
    }

    public boolean isLibraryMassMarket(String library) {
        return cryptoClassificationService.isLibraryMassMarket(library, "latest");
    }

    public List<ModuleAnalysis> getClassificationHistory(String moduleName) {
        return classificationHistoryRepository.findByModuleName(moduleName);
    }

    public void checkForClassificationChanges(String moduleName) {
        List<ModuleAnalysis> history = getClassificationHistory(moduleName);
        if (history.size() > 1) {
            ModuleAnalysis latest = history.get(history.size() - 1);
            ModuleAnalysis previous = history.get(history.size() - 2);
            if (!latest.getEccnClassification().equals(previous.getEccnClassification())) {
                sendClassificationChangeAlert(moduleName, previous.getEccnClassification(), latest.getEccnClassification());
            }
        }
    }

    private void sendClassificationChangeAlert(String moduleName, String oldClassification, String newClassification) {
        // Implementation to send alerts
    }

    private Map<String, Object> analyzeSourceCodeContent(String repositoryUrl, String branch, String commit) {
        // Implementation to analyze source code
        return Map.of("encryptionLibraries", List.of(), "hasPaymentProcessing", false);
    }

    private Map<String, Object> analyzePackageContent(String packageName, String packageType) {
        // Implementation to analyze software package
        return Map.of("encryptionLibraries", List.of(), "hasPaymentProcessing", false);
    }

    public interface AIModel {
        String classify(List<String> encryptionLibraries, boolean hasPaymentProcessing);
        Map<String, Double> suggestClassifications(List<String> encryptionLibraries, boolean hasPaymentProcessing);
    }
}