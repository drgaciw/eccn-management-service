package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.model.Eccn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.aciworldwide.eccn_management_service.model.Eccn.EccnHistoryEntry;

@Repository
public interface EccnRepository extends MongoRepository<Eccn, String> {
    List<Eccn> findByCommodityCode(String commodityCode);
    List<Eccn> findByCommodityCodeContainingIgnoreCase(String term);
    List<Eccn> findByCategory(String category);
    List<Eccn> findByEncryptionRelatedTrue();
    List<Eccn> findByFinancialSoftwareTrue();
    List<Eccn> findByDataAnalyticsTrueAndAnalyticsCapabilitiesIn(List<String> capabilities);
    List<Eccn> findByApplicableEARControlsIn(List<String> earControls);
    @Query("{ 'relatedEccns': ?0 }")
    List<Eccn> findRelatedEccns(String eccnId);
    @Query("{ '_id': ?0 }")
    List<EccnHistoryEntry> findEccnHistory(String eccnId);
    List<Eccn> findByReplacementEccnId(String replacementEccnId);
}