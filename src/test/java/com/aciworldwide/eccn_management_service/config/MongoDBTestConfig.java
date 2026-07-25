package com.aciworldwide.eccn_management_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class MongoDBTestConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoDBTestConfig.class);

    static {
        String dockerSocket = isWindows()
                ? "npipe:////./pipe/docker_engine"
                : "unix:///var/run/docker.sock";
        System.setProperty("testcontainers.docker.socket", dockerSocket);
        logger.info("Configured Testcontainers Docker socket: {}", dockerSocket);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
                .withLogConsumer(new Slf4jLogConsumer(logger))
                .withReuse(true);
    }
}
