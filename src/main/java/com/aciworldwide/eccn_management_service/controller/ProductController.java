package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.model.Product;
import com.aciworldwide.eccn_management_service.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product created = productService.createProduct(product);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @RequestBody Product product) {
        Product updated = productService.updateProduct(id, product);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Product>> getProductsByStatus(
            @PathVariable String status) {
        List<Product> products = productService.getProductsByStatus(status);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam String name) {
        List<Product> products = productService.searchProductsByName(name);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/pending-classification")
    public ResponseEntity<List<Product>> getProductsPendingClassification() {
        List<Product> products = productService.getProductsWithPendingClassification();
        return ResponseEntity.ok(products);
    }

    @PatchMapping("/{productId}/versions/{versionNumber}/mark-classified")
    public ResponseEntity<Void> markVersionAsClassified(
            @PathVariable String productId,
            @PathVariable String versionNumber) {
        productService.markVersionAsClassified(productId, versionNumber);
        return ResponseEntity.noContent().build();
    }
}