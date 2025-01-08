package com.aciworldwide.eccn_management_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;

@TestConfiguration
public class LoadBalancerTestConfig {

    @Bean
    @Primary
    ServiceInstanceListSupplier serviceInstanceListSupplier() {
        return ServiceInstanceListSuppliers.from("test-service");
    }
}