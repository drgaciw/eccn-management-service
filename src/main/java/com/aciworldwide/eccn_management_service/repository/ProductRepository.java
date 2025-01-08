package com.aciworldwide.eccn_management_service.repository;

import com.aciworldwide.eccn_management_service.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByStatus(String status);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByVersions_ClassificationStatus(String classificationStatus);
}