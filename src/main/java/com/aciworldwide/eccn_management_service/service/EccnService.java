package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.exception.EccnValidationException;
import com.aciworldwide.eccn_management_service.exception.EccnException;
import com.aciworldwide.eccn_management_service.exception.InvalidEccnFormatException;
import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.repository.EccnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EccnService {

    private final EccnRepository eccnRepository;
    private static final Pattern ECCN_PATTERN = Pattern.compile("^[0-9A-Z]{5}$");
    private static final Set<String> VALID_CATEGORIES = new HashSet<>(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
    private static final Set<String> VALID_SUBCATEGORIES = new HashSet<>(Arrays.asList("A", "B", "C", "D", "E"));
    private static final Set<String> VALID_CONTROL_REASONS = new HashSet<>(Arrays.asList("NS", "MT", "NP", "CB", "AT", "CC", "RS", "SI", "SL", "UN"));

    @Transactional
    @CacheEvict(value = "eccns", allEntries = true)
    public Eccn createEccn(Eccn eccn) {
        validateEccn(eccn);
        try {
            return eccnRepository.save(eccn);
        } catch (DuplicateKeyException e) {
            throw new EccnValidationException(
                "ECCN with this commodity code already exists: " + eccn.getCommodityCode(),
                EccnException.ErrorCodes.DUPLICATE_CODE
            );
        }
    }

    private void validateEccn(Eccn eccn) {
        if (eccn == null) {
            throw new EccnValidationException("ECCN cannot be null");
        }

        // Normalize aliases before validation:
        // - commodityCode may arrive as JsonAlias eccnCode
        // - code may be populated without commodityCode
        // - singular controlReason string (Angular form) → controlReasons list
        if (eccn.getCommodityCode() == null || eccn.getCommodityCode().isBlank()) {
            if (eccn.getCode() != null && !eccn.getCode().isBlank()) {
                eccn.setCommodityCode(eccn.getCode().trim());
            }
        }
        if (eccn.getCode() == null || eccn.getCode().isBlank()) {
            eccn.setCode(eccn.getCommodityCode());
        }
        if ((eccn.getControlReasons() == null || eccn.getControlReasons().isEmpty())
                && eccn.getControlReason() != null && !eccn.getControlReason().isBlank()) {
            List<String> reasons = Arrays.stream(eccn.getControlReason().split("[,;\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
            eccn.setControlReasons(reasons);
        }

        validateEccnCode(eccn.getCommodityCode());
        validateCategory(eccn.getCategory());
        validateSubCategory(eccn.getSubCategory());
        validateControlReasons(eccn.getControlReasons());
        validateDescription(eccn.getDescription());
    }

    private void validateEccnCode(String code) {
        if (code == null || code.isBlank()) {
            throw new EccnValidationException(
                    "commodityCode (or eccnCode) is required and must be 5 characters of numbers and uppercase letters");
        }
        if (!ECCN_PATTERN.matcher(code).matches()) {
            throw new InvalidEccnFormatException("Invalid ECCN code format. Must be 5 characters of numbers and uppercase letters.");
        }
    }

    private void validateCategory(String category) {
        if (category == null || !VALID_CATEGORIES.contains(category)) {
            throw new EccnValidationException("Invalid category. Must be a number between 0-9");
        }
    }

    private void validateSubCategory(String subCategory) {
        if (subCategory == null || !VALID_SUBCATEGORIES.contains(subCategory)) {
            throw new EccnValidationException("Invalid subcategory. Must be A, B, C, D, or E");
        }
    }

    private void validateControlReasons(List<String> controlReasons) {
        if (controlReasons == null || controlReasons.isEmpty()) {
            throw new EccnValidationException("At least one control reason is required");
        }

        for (String reason : controlReasons) {
            if (!VALID_CONTROL_REASONS.contains(reason)) {
                throw new EccnValidationException("Invalid control reason: " + reason);
            }
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.trim().length() < 10) {
            throw new EccnValidationException("Description must be at least 10 characters");
        }
        if (description.length() > 1000) {
            throw new EccnValidationException("Description cannot exceed 1000 characters");
        }
    }

    @Transactional
    @CacheEvict(value = "eccns", allEntries = true)
    public Eccn updateEccn(String id, Eccn eccn) {
        validateEccn(eccn);
        eccn.setId(id);
        return eccnRepository.save(eccn);
    }

    public List<Eccn> findByCommodityCode(String commodityCode) {
        validateEccnFormat(commodityCode);
        return eccnRepository.findByCommodityCode(commodityCode);
    }

    public List<Eccn> findFinancialSoftwareEccns() {
        return eccnRepository.findByFinancialSoftwareTrue();
    }

    public List<Eccn> findDataAnalyticsEccns(List<String> capabilities) {
        return eccnRepository.findByDataAnalyticsTrueAndAnalyticsCapabilitiesIn(capabilities);
    }

    public List<Eccn> findEccnsByEARControls(List<String> earControls) {
        return eccnRepository.findByApplicableEARControlsIn(earControls);
    }

    public List<Eccn> findAllEccns() {
        return eccnRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "eccns", allEntries = true)
    public void deprecateEccn(String eccnId, String reason, String replacementEccnId) {
        Eccn eccn = eccnRepository.findById(eccnId)
                .orElseThrow(() -> new IllegalArgumentException("ECCN not found"));
        eccn.setDeprecated(true);
        eccn.setDeprecationReason(reason);
        eccn.setReplacementEccnId(replacementEccnId);
        eccnRepository.save(eccn);
    }

    public Eccn getSupersedingEccn(String eccnId) {
        return eccnRepository.findById(eccnId)
                .map(Eccn::getReplacementEccnId)
                .flatMap(eccnRepository::findById)
                .orElse(null);
    }

    public List<Eccn> getRelatedEccns(String eccnId) {
        return eccnRepository.findRelatedEccns(eccnId);
    }

    public void validateEccnFormat(String eccn) {
        if (eccn == null || !ECCN_PATTERN.matcher(eccn).matches()) {
            throw new InvalidEccnFormatException("Invalid ECCN format: " + eccn);
        }
    }

    @CircuitBreaker(name = "eccnService", fallbackMethod = "getEccnHistoryFallback")
    public List<Eccn.EccnHistoryEntry> getEccnHistory(String eccnId) {
        return Collections.emptyList();
    }

    public List<Eccn.EccnHistoryEntry> getEccnHistoryFallback(String eccnId, Throwable t) {
        return Collections.emptyList();
    }

    @Transactional
    @CacheEvict(value = "eccns", allEntries = true)
    public List<Eccn> bulkCreateEccn(List<Eccn> eccns) {
        eccns.forEach(eccn -> validateEccnFormat(eccn.getCommodityCode()));
        return eccnRepository.saveAll(eccns);
    }

    public List<Eccn> findAll(Map<String, String> params) {
        // Handle filtering based on params
        if (params.containsKey("category")) {
            return eccnRepository.findByCategory(params.get("category"));
        }
        if (params.containsKey("controlReason")) {
            return eccnRepository.findByControlReasonsContaining(params.get("controlReason"));
        }
        // If no specific filters, return all
        return findAllEccns();
    }
    
    /**
     * Find an ECCN by its ID
     *
     * @param id The ID of the ECCN to find
     * @return The ECCN if found
     * @throws IllegalArgumentException if the ECCN is not found
     */
    @Cacheable(value = "eccns", key = "#id")
    public Eccn findById(String id) {
        return eccnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ECCN not found with ID: " + id));
    }
    
    /**
     * Search ECCNs by a query string.
     * Uses MongoDB regex query to search at the database level for better performance
     * and to avoid loading all records into memory.
     *
     * @param query The search query
     * @return A list of ECCNs matching the search criteria
     */
    public List<Eccn> searchEccns(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAllEccns();
        }
        
        // Use MongoDB regex search to avoid loading all records into memory
        return eccnRepository.searchByCodeOrDescription(query.trim());
    }
    
    /**
     * Delete an ECCN by its ID
     *
     * @param id The ID of the ECCN to delete
     */
    @Transactional
    @CacheEvict(value = "eccns", allEntries = true)
    public void deleteEccn(String id) {
        // Check if the ECCN exists first
        if (!eccnRepository.existsById(id)) {
            throw new IllegalArgumentException("Cannot delete non-existent ECCN with ID: " + id);
        }
        eccnRepository.deleteById(id);
    }
}
