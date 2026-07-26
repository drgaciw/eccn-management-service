package com.aciworldwide.eccn_management_service;

import com.aciworldwide.eccn_management_service.exception.EccnException;
import com.aciworldwide.eccn_management_service.exception.EccnValidationException;
import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.repository.EccnRepository;
import com.aciworldwide.eccn_management_service.service.EccnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.aciworldwide.eccn_management_service.config.MongoDBTestConfig;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression test that locks in the unique index on {@code Eccn.commodityCode}
 * and the duplicate-key handling in {@link EccnService#createEccn}. Runs against
 * the local MongoDB used by the test profile; the unique index is ensured
 * explicitly via {@link MongoTemplate} (auto-index-creation is not reliable in
 * this setup).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MongoDBTestConfig.class)
class EccnDuplicateKeyTest {

    @Autowired
    private EccnService eccnService;

    @Autowired
    private EccnRepository eccnRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.indexOps(Eccn.class).createIndex(
                new Index().on("commodityCode", Sort.Direction.ASC).unique());
        eccnRepository.deleteAll();
    }

    @Test
    void createEccn_withDuplicateCommodityCode_throwsValidationExceptionWithDuplicateCode() {
        String commodityCode = "5D002";
        eccnService.createEccn(validEccn(commodityCode, "Information security software description"));

        EccnValidationException exception = assertThrows(EccnValidationException.class,
                () -> eccnService.createEccn(validEccn(commodityCode, "Another valid encryption description")));

        assertEquals(EccnException.ErrorCodes.DUPLICATE_CODE, exception.getErrorCode());
    }

    @Test
    void createEccn_withUniqueCommodityCode_succeeds() {
        Eccn saved = eccnService.createEccn(validEccn("5E001", "Telecommunications software description"));

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("5E001", saved.getCommodityCode());
    }

    private Eccn validEccn(String commodityCode, String description) {
        Eccn eccn = new Eccn();
        eccn.setCommodityCode(commodityCode);
        eccn.setCategory("5");
        eccn.setSubCategory("D");
        eccn.setControlReasons(Arrays.asList("NS", "AT"));
        eccn.setDescription(description);
        return eccn;
    }
}
