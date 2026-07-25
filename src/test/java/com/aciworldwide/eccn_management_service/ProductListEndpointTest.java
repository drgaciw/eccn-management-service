package com.aciworldwide.eccn_management_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * UAT: Angular ProductListComponent issues GET /api/products. Without a list
 * mapping the backend returns 405 Method Not Allowed, so the product table
 * never shows live data (only mock fallback after the error interceptor runs).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductListEndpointTest {

    @LocalServerPort
    private int port;

    @Test
    void getProducts_returnsOkNotMethodNotAllowed() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Basic " + credentials)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertNotEquals(405, response.statusCode(),
                "GET /api/products must be supported for the Angular product list. Body: "
                        + response.body());
        assertEquals(200, response.statusCode(),
                "Authenticated GET /api/products should return 200. Body: " + response.body());
    }
}
