package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.Product;
import org.springframework.stereotype.Service;

@Service
public class CryptoClassificationService {

    public enum Algorithm {
        AES, RSA, ECC, BLOWFISH, SHA3
    }

    public enum Mode {
        ECB, CBC, GCM
    }

    public String classifyCryptography(int keyLength, Algorithm algorithm, boolean isMassMarket) {
        if (algorithm == null || keyLength < 64) {
            return "Not classified";
        }

        if (qualifiesForMassMarket(keyLength, algorithm)) {
            return "ECCN 5D992";
        } else if (qualifiesForControlledEncryption(keyLength, algorithm)) {
            return "ECCN 5D002";
        }
        return "Not classified";
    }

    public String classifyCryptoAlgorithm(Algorithm algorithm, int keyLength, Mode mode) {
        if (isRestrictedAlgorithm(algorithm)) {
            return "ECCN 5D002";
        }

        if (isWeakMode(mode)) {
            return "ECCN 5D002";
        }

        if (keyLength > 128) {
            return "ECCN 5D002";
        }

        return "ECCN 5D992";
    }

    public String classifyCryptoImplementation(String sourceCode, String language) {
        Algorithm algorithm = analyzeAlgorithm(sourceCode, language);
        boolean isMassMarket = analyzeMassMarket(sourceCode, language);
        return classifyCryptography(256, algorithm, isMassMarket);
    }

    public boolean assessDeMinimis(Product product) {
        if (product.isMilitaryUse() || product.isControlledCountry()) {
            return false;
        }
        return product.getUsContentPercentage() <= 25.0;
    }

    public String getEncryptionRegistrationNumber(Product product) {
        if (!product.isEncryptionEnabled()) {
            return null;
        }

        if (assessDeMinimis(product)) {
            return null;
        }

        return generateERN(product);
    }

    public String analyzeCryptoLibrary(String libraryName, String version) {
        Algorithm algorithm = getLibraryAlgorithm(libraryName, version);
        int keyLength = getLibraryKeyLength(libraryName, version);
        boolean isMassMarket = isLibraryMassMarket(libraryName, version);
        return classifyCryptography(keyLength, algorithm, isMassMarket);
    }

    public boolean qualifiesForMassMarket(int keyLength, Algorithm algorithm) {
        if (algorithm == null || isRestrictedAlgorithm(algorithm)) {
            return false;
        }
        return keyLength <= 128;
    }

    public boolean qualifiesForControlledEncryption(int keyLength, Algorithm algorithm) {
        if (algorithm == null) {
            return false;
        }
        if (isRestrictedAlgorithm(algorithm)) {
            return true;
        }
        return keyLength > 128;
    }

    public boolean isRestrictedAlgorithm(Algorithm algorithm) {
        return algorithm == Algorithm.SHA3;
    }

    public boolean isWeakMode(Mode mode) {
        return mode == Mode.ECB;
    }

    private Algorithm analyzeAlgorithm(String sourceCode, String language) {
        // Implementation to analyze algorithm from source code
        return Algorithm.AES; // Default value
    }

    private boolean analyzeMassMarket(String sourceCode, String language) {
        // Implementation to analyze mass market characteristics
        return false; // Default value
    }

    public int getLibraryKeyLength(String libraryName, String version) {
        // Implementation to get key length from library
        return 256; // Default value
    }

    public Algorithm getLibraryAlgorithm(String libraryName, String version) {
        // Implementation to get algorithm from library
        return Algorithm.AES; // Default value
    }

    public boolean isLibraryMassMarket(String libraryName, String version) {
        // Implementation to determine if library is mass market
        return false; // Default value
    }

    private String generateERN(Product product) {
        // Implementation to generate ERN
        return "ERN-" + product.getId();
    }
}