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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the UAT-observed bug where submitting an ECCN record with an
 * invalid code format returned an opaque "500 Internal Server Error" instead of a
 * validation-shaped 400.
 *
 * Root cause: {@link com.aciworldwide.eccn_management_service.exception.InvalidEccnFormatException}
 * extends bare {@code RuntimeException} rather than
 * {@link com.aciworldwide.eccn_management_service.exception.EccnException}, so it never
 * matches {@link com.aciworldwide.eccn_management_service.exception.GlobalExceptionHandler}'s
 * {@code handleEccnException} and falls through to the catch-all {@code Exception.class}
 * handler, which always answers 500. GitNexus impact analysis on
 * {@code InvalidEccnFormatException} reported HIGH risk (11 impacted symbols across
 * createEccn/updateEccn/bulkCreateEccn/findByCommodityCode), so this fix deliberately
 * leaves that class's type hierarchy untouched and instead adds a dedicated,
 * low-risk {@code @ExceptionHandler} for it in {@code GlobalExceptionHandler}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InvalidEccnFormatExceptionHandlingTest {

    @LocalServerPort
    private int port;

    @Test
    void createEccn_withInvalidCodeFormat_returnsBadRequestNotInternalServerError() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes());

        // commodityCode deliberately fails EccnService's ECCN_PATTERN (^[0-9A-Z]{5}$);
        // validateEccnCode() is the first check in validateEccn(), so this is the only
        // exception path exercised regardless of the other fields' values.
        String body = "{\"commodityCode\":\"bad-code\",\"category\":\"5\",\"subCategory\":\"D\","
                + "\"controlReasons\":[\"NS\"],\"description\":\"Test description\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/eccn"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + credentials)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode(),
                "Invalid ECCN code format must be reported as 400 Bad Request, not an opaque 500. Body: "
                        + response.body());
        assertTrue(response.body() != null && response.body().contains("Invalid ECCN code format"),
                "Response body should surface the validation message. Body: " + response.body());
    }
}
