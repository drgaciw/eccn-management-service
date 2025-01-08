package com.aciworldwide.eccn_management_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;
import java.util.Collections;
import java.util.List;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "test"));
    }

    @Bean
    @Primary
    public ServiceInstanceListSupplier serviceInstanceListSupplier() {
        ServiceInstance instance = new DefaultServiceInstance(
            "test-instance-1",
            "test-service",
            "localhost",
            8080,
            false
        );
        
        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "test-service";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(Collections.singletonList(instance));
            }
        };
    }
}