package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.model.Eccn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EccnRepository extends MongoRepository<Eccn, String> {
    List<Eccn> findByCommodityCode(String commodityCode);
    List<Eccn> findByCommodityCodeContainingIgnoreCase(String term);
    List<Eccn> findByEncryptionRelatedTrue();
    List<Eccn> findByFinancialSoftwareTrue();
    List<Eccn> findByDataAnalyticsTrueAndAnalyticsCapabilitiesIn(List<String> capabilities);
    List<Eccn> findByApplicableEARControlsIn(List<String> earControls);
    @Query("{ 'relatedEccns': ?0 }")
    List<Eccn> findRelatedEccns(String eccnId);
    List<Eccn> findByReplacementEccnId(String replacementEccnId);
    List<Eccn> findByCategory(String category);
    List<Eccn> findByControlReasonsContaining(String controlReason);
    
    /**
     * Search ECCNs by code or description using case-insensitive regex.
     * This performs the search at the database level to avoid loading all records into memory.
     * 
     * @param searchTerm the search term to look for in code or description
     * @return list of matching ECCNs
     */
    @Query("{$or: [{'code': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}, {'commodityCode': {$regex: ?0, $options: 'i'}}]}")
    List<Eccn> searchByCodeOrDescription(String searchTerm);
}