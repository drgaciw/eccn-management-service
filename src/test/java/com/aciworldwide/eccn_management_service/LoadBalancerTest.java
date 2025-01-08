package com.aciworldwide.eccn_management_service;

import com.aciworldwide.eccn_management_service.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
public class LoadBalancerTest {

    @Autowired
    private ServiceInstanceListSupplier serviceInstanceListSupplier;

    @Test
    void testServiceId() {
        String serviceId = serviceInstanceListSupplier.getServiceId();
        assertNotNull(serviceId);
        assertEquals("test-service", serviceId);
    }

    @Test
    void testServiceInstanceList() {
        StepVerifier.create(serviceInstanceListSupplier.get())
            .assertNext(instanceList -> {
                assertNotNull(instanceList);
                assertEquals(1, instanceList.size());
                
                ServiceInstance instance = instanceList.get(0);
                assertEquals("test-service", instance.getServiceId());
                assertEquals("localhost", instance.getHost());
                assertEquals(8080, instance.getPort());
            })
            .verifyComplete();
    }
}