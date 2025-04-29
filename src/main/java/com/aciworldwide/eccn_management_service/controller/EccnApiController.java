package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.repository.EccnRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/eccn")
public class EccnApiController {

    private final EccnRepository eccnRepository;

    public EccnApiController(EccnRepository eccnRepository) {
        this.eccnRepository = eccnRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllEccnCodes() {
        List<Eccn> eccnCodes = eccnRepository.findAll();
        
        // Convert to simplified format for the frontend
        List<Map<String, Object>> simplifiedEccnCodes = eccnCodes.stream()
            .map(eccn -> Map.of(
                "id", eccn.getId(),
                "commodityCode", eccn.getCommodityCode(),
                "description", eccn.getDescription(),
                "category", eccn.getCategory(),
                "encryptionRelated", eccn.isEncryptionRelated()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(simplifiedEccnCodes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Eccn> getEccnById(@PathVariable String id) {
        return eccnRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{commodityCode}")
    public ResponseEntity<List<Eccn>> getEccnByCommodityCode(@PathVariable String commodityCode) {
        List<Eccn> eccnCodes = eccnRepository.findByCommodityCode(commodityCode);
        return ResponseEntity.ok(eccnCodes);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Eccn>> getEccnByCategory(@PathVariable String category) {
        List<Eccn> eccnCodes = eccnRepository.findByCategory(category);
        return ResponseEntity.ok(eccnCodes);
    }

    @GetMapping("/encryption")
    public ResponseEntity<List<Eccn>> getEncryptionRelatedEccn() {
        List<Eccn> eccnCodes = eccnRepository.findByEncryptionRelatedTrue();
        return ResponseEntity.ok(eccnCodes);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Eccn>> searchEccn(@RequestParam String term) {
        List<Eccn> eccnCodes = eccnRepository.findByCommodityCodeContainingIgnoreCase(term);
        return ResponseEntity.ok(eccnCodes);
    }

    @PostMapping
    public ResponseEntity<Eccn> createEccn(@RequestBody Eccn eccn) {
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
