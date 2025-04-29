package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.repository.EccnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eccn-list")
public class EccnListController {

    private final EccnRepository eccnRepository;

    public EccnListController(EccnRepository eccnRepository) {
        this.eccnRepository = eccnRepository;
    }

    @GetMapping
    public ResponseEntity<List<Eccn>> getAllEccnCodes() {
        return ResponseEntity.ok(eccnRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Eccn> getEccnById(@PathVariable String id) {
        return eccnRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Eccn>> searchEccnCodes(@RequestParam String term) {
        // This is a simple implementation. In a real application, you might want to use
        // more sophisticated search techniques like full-text search
        List<Eccn> results = eccnRepository.findByCommodityCodeContainingIgnoreCase(term);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Eccn>> getEccnByCategory(@PathVariable String category) {
        List<Eccn> results = eccnRepository.findByCategory(category);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/encryption-related")
    public ResponseEntity<List<Eccn>> getEncryptionRelatedEccn() {
        List<Eccn> results = eccnRepository.findByEncryptionRelatedTrue();
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<Eccn> createEccn(@RequestBody Eccn eccn) {
        // Ensure we're creating a new record, not updating an existing one
        if (eccn.getId() != null && eccnRepository.existsById(eccn.getId())) {
            return ResponseEntity.badRequest().build();
        }
        Eccn savedEccn = eccnRepository.save(eccn);
        return ResponseEntity.ok(savedEccn);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Eccn> updateEccn(@PathVariable String id, @RequestBody Eccn eccn) {
        if (!eccnRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        eccn.setId(id);
        Eccn updatedEccn = eccnRepository.save(eccn);
        return ResponseEntity.ok(updatedEccn);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEccn(@PathVariable String id) {
        if (!eccnRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        eccnRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
