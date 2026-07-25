package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.model.ValidationResult;

import java.util.List;

/**
 * Service interface for validating ECCN codes against external databases.
 * Provides methods for validation, synchronization, and retrieving control reasons.
 */
public interface ExternalEccnValidationService {

    /**
     * Validates an ECCN code against an external database.
     *
     * @param eccnCode The ECCN code to validate
     * @return true if the ECCN code is valid, false otherwise
     */
    boolean validateEccnWithExternalDatabase(String eccnCode);

    /**
     * Retrieves the latest control reasons from the external database.
     *
     * @return List of control reason codes
     */
    List<String> getLatestControlReasons();

    /**
     * Synchronizes the local database with the external database.
     * This operation may take some time depending on the amount of data.
     */
    void syncLocalDatabase();

    /**
     * Performs a complete validation of an ECCN entity.
     *
     * @param eccn The ECCN entity to validate
     * @return ValidationResult containing validation status and any errors or warnings
     */
    ValidationResult validateCompleteEccn(Eccn eccn);
}