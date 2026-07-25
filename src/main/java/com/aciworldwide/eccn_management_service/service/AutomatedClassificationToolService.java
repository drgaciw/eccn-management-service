package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.repository.ClassificationHistoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class AutomatedClassificationToolService {
    private static final Logger logger = LoggerFactory.getLogger(AutomatedClassificationToolService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern ECCN_CODE = Pattern.compile("^(\\d[A-Z]\\d{3}|EAR99)$");

    private final ClassificationHistoryRepository classificationHistoryRepository;
    private final CryptoClassificationService cryptoClassificationService;
    private final ChatClient chatClient;

    public AutomatedClassificationToolService(
            ClassificationHistoryRepository classificationHistoryRepository,
            CryptoClassificationService cryptoClassificationService,
            @Autowired(required = false) @Qualifier("claudeClient") ChatClient chatClient) {
        this.classificationHistoryRepository = Objects.requireNonNull(classificationHistoryRepository, "ClassificationHistoryRepository must not be null");
        this.cryptoClassificationService = Objects.requireNonNull(cryptoClassificationService, "CryptoClassificationService must not be null");
        this.chatClient = chatClient;
        if (chatClient == null) {
            logger.warn("ChatClient not configured — AI-based classification will be unavailable. "
                + "Set ANTHROPIC_API_KEY to enable LLM classification features.");
        }
    }

    @Document
    public static class ModuleAnalysis {
        @Id
        private String id;
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

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
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

    public boolean validateClassification(String moduleName, String proposedECCN) {
        Objects.requireNonNull(moduleName, "Module name cannot be null");
        Objects.requireNonNull(proposedECCN, "Proposed ECCN cannot be null");

        ModuleAnalysis analysis = getLatestAnalysis(moduleName);
        if (analysis == null) {
            logger.warn("No analysis found for module: {}", moduleName);
            return false;
        }

        String classification = determineClassification(analysis.getEncryptionLibraries(), analysis.isHasPaymentProcessing());
        if (classification == null) {
            logger.warn("Could not determine classification for module: {}", moduleName);
            return false;
        }

        return classification.equals(proposedECCN);
    }

    public Map<String, Double> suggestECCN(String moduleName) {
        Objects.requireNonNull(moduleName, "Module name cannot be null");

        ModuleAnalysis analysis = getLatestAnalysis(moduleName);
        if (analysis == null) {
            logger.warn("No analysis available for module: {}", moduleName);
            return Map.of();
        }
        if (chatClient == null) {
            logger.warn("ChatClient not configured for module: {}", moduleName);
            return Map.of();
        }
        try {
            String response = chatClient.prompt()
                .user(getSuggestionPrompt(analysis))
                .call()
                .content();
            return parseSuggestions(response);
        } catch (Exception e) {
            logger.error("AI suggestion failed for {}: {}", moduleName, e.getMessage());
            return Map.of();
        }
    }

    private String getSuggestionPrompt(ModuleAnalysis analysis) {
        return String.format(
            "Suggest ECCN classifications with confidence scores (0.0-1.0) for a module with: "
            + "encryption libraries=%s, payment processing=%s, type=%s. "
            + "Respond with JSON: {\"5A002\": 0.85, \"5D992\": 0.60, \"EAR99\": 0.40}",
            analysis.getEncryptionLibraries(), analysis.isHasPaymentProcessing(),
            analysis.getModuleType());
    }

    private Map<String, Double> parseSuggestions(String response) {
        try {
            String json = response.trim();
            int braceStart = json.indexOf('{');
            int braceEnd = json.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                return MAPPER.readValue(json.substring(braceStart, braceEnd + 1),
                        new TypeReference<LinkedHashMap<String, Double>>() {});
            }
        } catch (Exception e) {
            logger.warn("Failed to parse AI suggestion response ({} chars)", response == null ? 0 : response.length());
        }
        return new LinkedHashMap<>();
    }

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

    @Transactional
    public ModuleAnalysis performClassification(ModuleAnalysis analysis) {
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

    public String determineClassification(List<String> encryptionLibraries, boolean hasPaymentProcessing) {
        if (hasNoEncryptionLibraries(encryptionLibraries)) {
            return determineClassificationForNoEncryption(hasPaymentProcessing);
        }

        String aiClassification = attemptAIClassification(encryptionLibraries, hasPaymentProcessing);
        if (aiClassification != null) {
            return aiClassification;
        }

        String cryptoClassification = attemptCryptoServiceClassification(encryptionLibraries);
        if (cryptoClassification != null) {
            return cryptoClassification;
        }

        return determineFallbackClassification(hasPaymentProcessing);
    }

    private boolean hasNoEncryptionLibraries(List<String> encryptionLibraries) {
        return encryptionLibraries == null || encryptionLibraries.isEmpty();
    }

    private String determineClassificationForNoEncryption(boolean hasPaymentProcessing) {
        return hasPaymentProcessing ? "5A002" : "EAR99";
    }

    private String attemptAIClassification(List<String> encryptionLibraries, boolean hasPaymentProcessing) {
        if (chatClient == null) {
            return null;
        }
        try {
            String prompt = String.format(
                "Classify this software module for ECCN export control. "
                + "Encryption libraries: %s. Payment processing: %s. "
                + "Return ONLY a single ECCN code (e.g., 5A002, 5D992, EAR99) with no explanation.",
                encryptionLibraries, hasPaymentProcessing);
            String response = chatClient.prompt().user(prompt).call().content();
            if (response == null) {
                return null;
            }
            String trimmed = response.trim();
            if (!ECCN_CODE.matcher(trimmed).matches()) {
                logger.warn("AI classification returned non-ECCN response, falling back to deterministic classification");
                return null;
            }
            return trimmed;
        } catch (Exception e) {
            logger.error("AI classification failed: {}", e.getMessage());
            return null;
        }
    }

    private String attemptCryptoServiceClassification(List<String> encryptionLibraries) {
        for (String library : encryptionLibraries) {
            String classification = classifySingleLibrary(library);
            if (classification != null) {
                return classification;
            }
        }
        return null;
    }

    private String classifySingleLibrary(String library) {
        return cryptoClassificationService.classifyCryptography(
                getLibraryKeyLength(library),
                getAlgorithmForLibrary(library),
                false
        );
    }

    private String determineFallbackClassification(boolean hasPaymentProcessing) {
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
    }

    private Map<String, Object> analyzeSourceCodeContent(String repositoryUrl, String branch, String commit) {
        return Map.of("encryptionLibraries", List.of(), "hasPaymentProcessing", false);
    }

    private Map<String, Object> analyzePackageContent(String packageName, String packageType) {
        return Map.of("encryptionLibraries", List.of(), "hasPaymentProcessing", false);
    }
}
