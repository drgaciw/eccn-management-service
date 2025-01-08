package com.aciworldwide.eccn_management_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableMongoRepositories
@EnableMongoAuditing
@EnableCaching
@EnableAsync
@EnableConfigurationProperties
@Slf4j
@ConditionalOnProperty(name = "spring.mongodb.enabled", havingValue = "true", matchIfMissing = true)
public class EccnManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EccnManagementServiceApplication.class, args);
		log.info("ECCN Management Service started successfully");
	}

}
