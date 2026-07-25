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
 * Regression test for the UAT-observed bug where every state-changing request from the
 * real Angular dev server (http://localhost:4200) was rejected by Spring's CORS filter
 * with "403 Invalid CORS request", before Spring Security or the controller ever ran.
 *
 * Browsers attach an Origin header to POST/PUT/PATCH/DELETE requests even when the
 * request is routed same-origin through a dev-server proxy, so any mismatch between the
 * frontend's actual origin and {@link com.aciworldwide.eccn_management_service.config.WebConfig}'s
 * allowed origins silently breaks every create/update/delete flow while GET requests
 * (which browsers do not tag with Origin) keep working — exactly the asymmetry seen
 * during manual UAT (ECCN create and classification submit both 403'd).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CorsConfigurationTest {

    private static final String DEV_FRONTEND_ORIGIN = "http://localhost:4200";

    @LocalServerPort
    private int port;

    @Test
    void requestFromDevFrontendOrigin_isNotRejectedByCorsFilter() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/eccn"))
                .header("Origin", DEV_FRONTEND_ORIGIN)
                .header("Authorization", "Basic " + credentials)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Spring's CorsFilter returns 403 "Invalid CORS request" for disallowed origins,
        // independent of authentication. Any other status means the request reached
        // Spring Security / the controller, i.e. the origin was accepted.
        assertNotEquals(403, response.statusCode(),
                "Requests from the dev frontend origin (" + DEV_FRONTEND_ORIGIN
                        + ") must not be rejected by the CORS filter. Body: " + response.body());
        assertEquals(DEV_FRONTEND_ORIGIN,
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }
}
