package com.aciworldwide.eccn_management_service.config;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4jBulkheadProvider;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class CircuitBreakerTestConfig {

    @Bean
    @Primary
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    @Primary
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }

    @Bean
    @Primary
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry() {
        return ThreadPoolBulkheadRegistry.ofDefaults();
    }

    @Bean
    @Primary
    public BulkheadRegistry bulkheadRegistry() {
        return BulkheadRegistry.ofDefaults();
    }

    @Bean
    @Primary
    public Resilience4JConfigurationProperties resilience4JConfigurationProperties() {
        return new Resilience4JConfigurationProperties();
    }

    @Bean
    @Primary
    public Resilience4jBulkheadProvider bulkheadProvider(
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
            BulkheadRegistry bulkheadRegistry,
            Resilience4JConfigurationProperties properties) {
        return new Resilience4jBulkheadProvider(
            threadPoolBulkheadRegistry,
            bulkheadRegistry,
            properties
        );
    }

    @Bean
    @Primary
    public Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            Resilience4jBulkheadProvider bulkheadProvider,
            Resilience4JConfigurationProperties properties) {
        return new Resilience4JCircuitBreakerFactory(
            circuitBreakerRegistry,
            timeLimiterRegistry,
            bulkheadProvider,
            properties
        );
    }
}