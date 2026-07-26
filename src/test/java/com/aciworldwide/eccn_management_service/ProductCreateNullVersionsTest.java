package com.aciworldwide.eccn_management_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.aciworldwide.eccn_management_service.config.MongoDBTestConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * UAT: Angular product create form posts {name, version, category, description}
 * without a {@code versions} array. ProductService.createProduct NPE'd on
 * {@code product.getVersions().forEach(...)} and the UI showed a generic 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MongoDBTestConfig.class)
class ProductCreateNullVersionsTest {

    @LocalServerPort
    private int port;

    @Test
    void createProduct_withoutVersions_doesNotReturn500() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        String body = """
                {"name":"UAT Null Versions Product","description":"from UI form"}
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertNotEquals(500, response.statusCode(),
                "createProduct must tolerate a null versions list. Body: " + response.body());
        assertEquals(201, response.statusCode(),
                "Expected CREATED. Body: " + response.body());
    }
}
