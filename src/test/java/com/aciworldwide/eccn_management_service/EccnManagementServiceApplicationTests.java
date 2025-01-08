package com.aciworldwide.eccn_management_service;

import com.aciworldwide.eccn_management_service.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
class EccnManagementServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
