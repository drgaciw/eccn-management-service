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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SIT: Angular product "View" and REST clients need GET /api/products/{id}.
 * Without it the API returns 405 Method Not Allowed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MongoDBTestConfig.class)
class ProductGetByIdEndpointTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void getProductById_returnsCreatedProduct() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        String name = "SIT GetById " + UUID.randomUUID();
        String createBody = """
                {"name":"%s","description":"for get by id"}
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
        String id = created.body().replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertTrue(id.length() > 5, "expected id in body: " + created.body());

        HttpResponse<String> got = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/products/" + id))
                        .header("Authorization", "Basic " + credentials)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertNotEquals(405, got.statusCode(), "GET by id must be supported. Body: " + got.body());
        assertEquals(200, got.statusCode(), got.body());
        assertTrue(got.body().contains(name), got.body());
    }

    @Test
    void getProductById_missing_returns404() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        HttpResponse<String> got = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/products/000000000000000000000000"))
                        .header("Authorization", "Basic " + credentials)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, got.statusCode(), got.body());
    }

    @Test
    void deleteProductById_returns204() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        String name = "SIT Delete " + UUID.randomUUID();
        HttpResponse<String> created = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/products"))
                        .header("Authorization", "Basic " + credentials)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"name\":\"" + name + "\",\"description\":\"to delete\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, created.statusCode(), created.body());
        String id = created.body().replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        HttpResponse<String> deleted = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/products/" + id))
                        .header("Authorization", "Basic " + credentials)
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertNotEquals(405, deleted.statusCode(), deleted.body());
        assertEquals(204, deleted.statusCode(), deleted.body());

        HttpResponse<String> got = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/products/" + id))
                        .header("Authorization", "Basic " + credentials)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, got.statusCode(), got.body());
    }
}
