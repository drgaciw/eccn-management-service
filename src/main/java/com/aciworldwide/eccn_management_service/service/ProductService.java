package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.events.ProductEvent;
import com.aciworldwide.eccn_management_service.model.Product;
import com.aciworldwide.eccn_management_service.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Product createProduct(Product product) {
        // Default status/classification status for versions, but only when the caller
        // didn't supply one. UAT3: POSTing a product with an explicit status (e.g.
        // "DEPRECATED") was silently forced to "ACTIVE" because this unconditionally
        // overwrote whatever the caller sent — the status never persisted as supplied.
        // The Angular create form does not send a versions array — tolerate null/empty
        // so create does not NPE into a generic 500 for the UI.
        if (product.getStatus() == null || product.getStatus().isBlank()) {
            product.setStatus("ACTIVE");
        }
        if (product.getVersions() == null) {
            product.setVersions(java.util.Collections.emptyList());
        } else {
            product.getVersions().forEach(version ->
                version.setClassificationStatus("PENDING"));
        }
        if (product.getFeatures() == null) {
            product.setFeatures(java.util.Collections.emptyList());
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(String id, Product product) {
        Product existing = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        // Preserve existing classification statuses
        product.getVersions().forEach(newVersion -> {
            existing.getVersions().stream()
                .filter(v -> v.getVersionNumber().equals(newVersion.getVersionNumber()))
                .findFirst()
                .ifPresent(existingVersion ->
                    newVersion.setClassificationStatus(existingVersion.getClassificationStatus()));
        });
        
        product.setId(id);
        Product updatedProduct = productRepository.save(product);
        
        // Publish event for each updated version
        product.getVersions().forEach(version ->
            eventPublisher.publishEvent(
                new ProductEvent(
                    ProductEvent.EventType.VERSION_UPDATED,
                    updatedProduct,
                    version.getVersionNumber())));
        
        return updatedProduct;
    }

    /**
     * Returns every product. Used by the Angular product list (GET /api/products).
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    @Transactional
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    public List<Product> getProductsByStatus(String status) {
        return productRepository.findByStatus(status);
    }

    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Product> getProductsWithPendingClassification() {
        return productRepository.findByVersions_ClassificationStatus("PENDING");
    }

    @Transactional
    public void markVersionAsClassified(String productId, String versionNumber) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        product.getVersions().stream()
            .filter(v -> v.getVersionNumber().equals(versionNumber))
            .findFirst()
            .ifPresent(version -> {
                version.setClassificationStatus("CLASSIFIED");
                productRepository.save(product);
                eventPublisher.publishEvent(
                    new ProductEvent(
                        ProductEvent.EventType.VERSION_CLASSIFIED,
                        product,
                        versionNumber));
            });
    }
}