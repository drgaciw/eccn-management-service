package com.aciworldwide.eccn_management_service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();
        
        circuitBreaker = CircuitBreaker.of("test", config);
    }

    @Test
    void testCircuitBreakerTransitionToOpenState() {
        // Simulate failures
        for (int i = 0; i < 2; i++) {
            circuitBreaker.onError(0, TimeUnit.MILLISECONDS, new RuntimeException());
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }
}