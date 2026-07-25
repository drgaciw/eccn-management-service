package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.model.GlossaryEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlossaryEntryRepository extends MongoRepository<GlossaryEntry, String> {
    
    Optional<GlossaryEntry> findByTerm(String term);
    
    List<GlossaryEntry> findByCategory(String category);
    
    @Query("{'term': {$regex: ?0, $options: 'i'}}")
    List<GlossaryEntry> findByTermContainingIgnoreCase(String termPart);
    
    @Query("{'definition': {$regex: ?0, $options: 'i'}}")
    List<GlossaryEntry> findByDefinitionContainingIgnoreCase(String text);
    
    List<GlossaryEntry> findByRegulatoryContextContainingIgnoreCase(String context);
    
    @Query("{'crossReferences': {$in: [?0]}}")
    List<GlossaryEntry> findByCrossReference(String reference);
    
    void deleteByTerm(String term);
}