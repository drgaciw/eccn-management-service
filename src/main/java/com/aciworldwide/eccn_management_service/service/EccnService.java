package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.exception.InvalidEccnFormatException;
import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.repository.EccnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
@RequiredArgsConstructor
public class EccnService {

    private final EccnRepository eccnRepository;
    private static final Pattern ECCN_PATTERN = Pattern.compile("^[0-9A-Z]{5}$");

    public Eccn createEccn(Eccn eccn) {
        validateEccnFormat(eccn.getCommodityCode());
        return eccnRepository.save(eccn);
    }

    public Eccn updateEccn(String id, Eccn eccn) {
        validateEccnFormat(eccn.getCommodityCode());
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
        if (!ECCN_PATTERN.matcher(eccn).matches()) {
            throw new InvalidEccnFormatException("Invalid ECCN format: " + eccn);
        }
    }

    @CircuitBreaker(name = "eccnService", fallbackMethod = "getEccnHistoryFallback")
    public List<Eccn.EccnHistoryEntry> getEccnHistory(String eccnId) {
        return eccnRepository.findEccnHistory(eccnId);
    }

    public List<Eccn.EccnHistoryEntry> getEccnHistoryFallback(String eccnId) {
        return Collections.emptyList();
    }

    public List<Eccn> bulkCreateEccn(List<Eccn> eccns) {
        eccns.forEach(eccn -> validateEccnFormat(eccn.getCommodityCode()));
        return eccnRepository.saveAll(eccns);
    }
}