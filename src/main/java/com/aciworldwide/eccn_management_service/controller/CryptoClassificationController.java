package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.service.CryptoClassificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crypto-classification")
@RequiredArgsConstructor
public class CryptoClassificationController {

    private final CryptoClassificationService cryptoClassificationService;

    @PostMapping("/classify")
    public ResponseEntity<String> classifyCryptography(
            @RequestParam int keyLength,
            @RequestParam String algorithm,
            @RequestParam(defaultValue = "false") boolean isMassMarket) {
        CryptoClassificationService.Algorithm algorithmEnum =
            CryptoClassificationService.Algorithm.valueOf(algorithm.toUpperCase());
        String classification = cryptoClassificationService.classifyCryptography(keyLength, algorithmEnum, isMassMarket);
        return ResponseEntity.ok(classification);
    }
}