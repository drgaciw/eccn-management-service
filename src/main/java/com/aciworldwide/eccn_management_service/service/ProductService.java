package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.events.ProductEvent;
import com.aciworldwide.eccn_management_service.model.Product;
import com.aciworldwide.eccn_management_service.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
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
        // Set initial status and classification status for versions
        product.setStatus("ACTIVE");
        product.getVersions().forEach(version -> 
            version.setClassificationStatus("PENDING"));
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