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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UAT3: POSTing a product with an explicit {@code status} (e.g. "DEPRECATED") must
 * persist that status. Observed live: {@code POST /api/products} with
 * {@code "status":"DEPRECATED"} for "Analytics Engine" but {@code GET /api/products}
 * returned {@code status: "ACTIVE"} for every product, seeded or not.
 * Root cause: {@link com.aciworldwide.eccn_management_service.service.ProductService#createProduct}
 * unconditionally overwrites {@code product.status} to {@code "ACTIVE"}, ignoring any
 * caller-supplied value.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MongoDBTestConfig.class)
class ProductCreateStatusPersistenceTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void createProduct_withExplicitStatus_persistsSuppliedStatus() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        String name = "UAT Deprecated Widget " + UUID.randomUUID();
        String createBody = """
                {"name":"%s","description":"created DEPRECATED","status":"DEPRECATED"}
                """.formatted(name);

        HttpResponse<String> created = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/products"))
                        .header("Authorization", "Basic " + credentials)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(createBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(201, created.statusCode(), created.body());
        assertTrue(created.body().contains("\"status\":\"DEPRECATED\""),
                "createProduct must persist the caller-supplied status instead of forcing ACTIVE. Body: "
                        + created.body());

        String id = created.body().replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        HttpResponse<String> got = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/products/" + id))
                        .header("Authorization", "Basic " + credentials)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, got.statusCode(), got.body());
        assertTrue(got.body().contains("\"status\":\"DEPRECATED\""),
                "GET must reflect the persisted DEPRECATED status, not ACTIVE. Body: " + got.body());
    }

    @Test
    void createProduct_withoutStatus_defaultsToActive() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        String name = "UAT Default Status Widget " + UUID.randomUUID();
        String createBody = """
                {"name":"%s","description":"no status supplied"}
                """.formatted(name);

        HttpResponse<String> created = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/products"))
                        .header("Authorization", "Basic " + credentials)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(createBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(201, created.statusCode(), created.body());
        assertTrue(created.body().contains("\"status\":\"ACTIVE\""),
                "createProduct must default status to ACTIVE when the caller supplies none. Body: "
                        + created.body());
    }
}
