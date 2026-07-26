package com.aciworldwide.eccn_management_service;

import com.aciworldwide.eccn_management_service.config.MongoDBTestConfig;
import com.aciworldwide.eccn_management_service.model.Product;
import com.aciworldwide.eccn_management_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Angular product-create form hardcodes its category options. It should
 * populate them from the database instead, via GET /api/products/categories.
 *
 * Two things this pins down:
 *  - /categories must not be swallowed by the /{id} path variable on the same
 *    controller, which would return 404 for a product id of "categories".
 *  - the response is the distinct, blank-free set actually present in the data,
 *    so the dropdown never offers a category no product uses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MongoDBTestConfig.class)
class ProductCategoriesEndpointTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductRepository productRepository;

    private static final String CREDENTIALS =
            Base64.getEncoder().encodeToString("admin:admin".getBytes());

    @BeforeEach
    void seed() {
        productRepository.deleteAll();
        productRepository.saveAll(List.of(
                product("PayGate", "Cryptography"),
                product("RiskEngine", "Cryptography"),   // duplicate -> collapses
                product("LedgerSync", "Networking"),
                product("Untagged", null),               // null -> excluded
                product("Blank", "   ")                  // blank -> excluded
        ));
    }

    private Product product(String name, String category) {
        Product p = new Product();
        p.setName(name);
        p.setStatus("ACTIVE");
        p.setCategory(category);
        return p;
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Basic " + CREDENTIALS)
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void categoriesEndpoint_isRoutable_notShadowedByIdPathVariable() throws Exception {
        HttpResponse<String> response = get("/api/products/categories");

        assertEquals(200, response.statusCode(),
                "GET /api/products/categories should route to the lookup, not the /{id} handler");
    }

    @Test
    void categoriesEndpoint_returnsDistinctNonBlankCategories() throws Exception {
        String body = get("/api/products/categories").body();

        assertTrue(body.contains("Cryptography"), "expected Cryptography in " + body);
        assertTrue(body.contains("Networking"), "expected Networking in " + body);
        assertFalse(body.contains("null"), "null category must be excluded: " + body);

        // "Cryptography" is stored twice but must appear once.
        int first = body.indexOf("Cryptography");
        assertEquals(first, body.lastIndexOf("Cryptography"),
                "duplicate categories must collapse: " + body);
    }

    @Test
    void categoriesEndpoint_returnsEmptyArrayWhenNoProductIsCategorised() throws Exception {
        productRepository.deleteAll();

        HttpResponse<String> response = get("/api/products/categories");

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body().trim(),
                "with no categorised products the lookup returns an empty array, "
                        + "letting the UI fall back to its own options");
    }
}
