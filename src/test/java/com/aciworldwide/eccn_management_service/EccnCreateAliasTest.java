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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SIT: Angular ECCN create form posts eccnCode / controlReason (singular).
 * Backend historically only accepted commodityCode + controlReasons list, so UI
 * create always failed with a confusing INVALID_FORMAT on null commodityCode.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EccnCreateAliasTest {

    @LocalServerPort
    private int port;

    @Test
    void createAcceptsEccnCodeAliasAndSingularControlReason() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        // Unique 5-char code ending in random hex-ish chars — use fixed pattern + digit
        String code = "5A" + String.format("%03d", (int)(Math.random() * 900 + 100)); // e.g. 5A742 — may fail pattern if has digits only... pattern is [0-9A-Z]{5}
        // better: 5A00X style - use 5A00 + letter from random
        char c = (char) ('A' + (int)(Math.random() * 26));
        code = "5A00" + c;

        String body = """
                {"eccnCode":"%s","category":"5","subCategory":"A","description":"Alias SIT information security classification","controlReason":"NS, AT"}
                """.formatted(code);

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/v1/eccn"))
                        .header("Authorization", "Basic " + credentials)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), response.body());
        assertTrue(response.body().contains(code) || response.body().contains("commodityCode"),
                response.body());
    }
}
