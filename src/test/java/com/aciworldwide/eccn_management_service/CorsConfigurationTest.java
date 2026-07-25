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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for UAT-observed CORS failures:
 * <ul>
 *   <li>State-changing requests from {@code http://localhost:4200} (ng serve)
 *       were rejected with "403 Invalid CORS request".</li>
 *   <li>Playwright/Chrome often navigate via {@code http://127.0.0.1:4200}, which
 *       is a different Origin and must also be allowed.</li>
 *   <li>Without {@code http.cors()} on the Security filter chain, OPTIONS preflight
 *       was answered with 401 Basic challenge instead of CORS headers.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CorsConfigurationTest {

    @LocalServerPort
    private int port;

    @Test
    void getFromLocalhost4200_isNotRejectedByCorsFilter() throws Exception {
        assertOriginAccepted("http://localhost:4200");
    }

    @Test
    void getFromLoopback4200_isNotRejectedByCorsFilter() throws Exception {
        assertOriginAccepted("http://127.0.0.1:4200");
    }

    @Test
    void preflightPostFromLoopback4200_returnsCorsHeadersNot401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Origin", "http://127.0.0.1:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization,x-api-version")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertNotEquals(401, response.statusCode(),
                "OPTIONS preflight must not require Basic auth. Body: " + response.body());
        assertNotEquals(403, response.statusCode(),
                "OPTIONS preflight must not be rejected by CORS. Body: " + response.body());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "OPTIONS preflight should succeed. Status=" + response.statusCode()
                        + " body=" + response.body());
        assertEquals("http://127.0.0.1:4200",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        String methods = response.headers().firstValue("Access-Control-Allow-Methods").orElse("");
        assertTrue(methods.toUpperCase().contains("POST"),
                "Allow-Methods should include POST, got: " + methods);
    }

    private void assertOriginAccepted(String origin) throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/eccn"))
                .header("Origin", origin)
                .header("Authorization", "Basic " + credentials)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertNotEquals(403, response.statusCode(),
                "Requests from " + origin + " must not be rejected by CORS. Body: " + response.body());
        assertEquals(origin,
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }
}
