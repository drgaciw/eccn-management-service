package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.service.CryptoClassificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crypto-classification")
public class CryptoClassificationController {

    private final CryptoClassificationService cryptoClassificationService;

    public CryptoClassificationController(CryptoClassificationService cryptoClassificationService) {
        this.cryptoClassificationService = cryptoClassificationService;
    }

    @PostMapping("/classify")
    public String classifyCryptography(
            @RequestParam int keyLength,
            @RequestParam String algorithm,
            @RequestParam(defaultValue = "false") boolean isMassMarket) {
        CryptoClassificationService.Algorithm algorithmEnum =
            CryptoClassificationService.Algorithm.valueOf(algorithm.toUpperCase());
        return cryptoClassificationService.classifyCryptography(keyLength, algorithmEnum, isMassMarket);
    }
}