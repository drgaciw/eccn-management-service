package com.aciworldwide.eccn_management_service.events;

import com.aciworldwide.eccn_management_service.model.Product;
import lombok.Data;

import java.time.Instant;

@Data
public class ProductEvent {
    public enum EventType {
        PRODUCT_CREATED,
        VERSION_UPDATED,
        VERSION_CLASSIFIED
    }

    private EventType eventType;
    private Product product;
    private String versionNumber; // Relevant for version-specific events
    private Instant timestamp;

    public ProductEvent(EventType eventType, Product product) {
        this.eventType = eventType;
        this.product = product;
        this.timestamp = Instant.now();
    }

    public ProductEvent(EventType eventType, Product product, String versionNumber) {
        this(eventType, product);
        this.versionNumber = versionNumber;
    }
}