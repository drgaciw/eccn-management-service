package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.model.ExportControl;
import com.aciworldwide.eccn_management_service.repository.ExportControlRepository;
import com.aciworldwide.eccn_management_service.service.ExportControlService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/export-control")
public class ExportControlController {

    private final ExportControlRepository exportControlRepository;
    private final MongoTemplate mongoTemplate;
    private final MongoClient mongoClient;

    @Autowired
    public ExportControlController(
            ExportControlRepository exportControlRepository,
            MongoTemplate mongoTemplate,
            MongoClient mongoClient) {
        this.exportControlRepository = exportControlRepository;
        this.mongoTemplate = mongoTemplate;
        this.mongoClient = mongoClient;
    }

    @GetMapping
    public ResponseEntity<List<ExportControl>> getAllExportControls() {
        return ResponseEntity.ok(exportControlRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExportControl> getExportControlById(@PathVariable String id) {
        return exportControlRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        Map<String, Object> dashboardData = new HashMap<>();
        
        // Get MongoDB database
        MongoDatabase database = mongoClient.getDatabase("eccn_management");
        
        // Count pending transactions
        MongoCollection<Document> transactionsCollection = database.getCollection("export_transactions");
        long pendingTransactions = transactionsCollection.countDocuments(
                new Document("status", "PENDING_APPROVAL"));
        
        // Get expiring licenses (within next 90 days)
        MongoCollection<Document> licensesCollection = database.getCollection("export_licenses");
        Date ninetyDaysFromNow = Date.from(LocalDate.now().plusDays(90)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        long expiringLicenses = licensesCollection.countDocuments(
                new Document("expirationDate", 
                        new Document("$lt", ninetyDaysFromNow))
                        .append("status", "ACTIVE"));
        
        // Count pending screenings
        MongoCollection<Document> customersCollection = database.getCollection("customers");
        long pendingScreenings = customersCollection.countDocuments(
                new Document("screeningStatus", "PENDING"));
        
        // Get recent transactions
        List<Document> recentTransactions = transactionsCollection.find()
                .sort(new Document("transactionDate", -1))
                .limit(5)
                .into(new ArrayList<>());
        
        // Get license utilization
        List<Document> licenseUtilization = licensesCollection.find(
                new Document("status", "ACTIVE"))
                .limit(5)
                .into(new ArrayList<>())
                .stream()
                .map(license -> {
                    Document utilization = new Document();
                    utilization.put("licenseId", license.getObjectId("_id").toString());
                    utilization.put("licenseNumber", license.getString("licenseNumber"));
                    utilization.put("licenseType", license.getString("licenseType"));
                    utilization.put("totalValue", license.get("valueLimit", 0));
                    utilization.put("usedValue", license.get("valueUsed", 0));
                    
                    // Calculate utilization percentage
                    double totalValue = license.get("valueLimit", 0.0);
                    double usedValue = license.get("valueUsed", 0.0);
                    double utilizationPercentage = totalValue > 0 ? (usedValue / totalValue) * 100 : 0;
                    utilization.put("utilizationPercentage", utilizationPercentage);
                    
                    // Calculate days remaining
                    Date expirationDate = license.getDate("expirationDate");
                    utilization.put("expirationDate", expirationDate);
                    
                    long daysRemaining = 0;
                    if (expirationDate != null) {
                        LocalDate expDate = expirationDate.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        daysRemaining = LocalDate.now().until(expDate).getDays();
                    }
                    utilization.put("daysRemaining", daysRemaining);
                    
                    return utilization;
                })
                .collect(Collectors.toList());
        
        // Get transactions by country
        List<Document> transactionsByCountry = new ArrayList<>();
        // This would typically be an aggregation, but for simplicity:
        Map<String, Document> countryMap = new HashMap<>();
        
        for (Document transaction : transactionsCollection.find().into(new ArrayList<>())) {
            String destination = transaction.getString("destination");
            if (destination == null) continue;
            
            Document countryData = countryMap.getOrDefault(destination, 
                    new Document("countryCode", destination)
                            .append("countryName", getCountryName(destination))
                            .append("transactionCount", 0)
                            .append("totalValue", 0.0));
            
            countryData.put("transactionCount", countryData.getInteger("transactionCount") + 1);
            
            Double transactionValue = transaction.getDouble("totalValue");
            if (transactionValue != null) {
                countryData.put("totalValue", 
                        countryData.getDouble("totalValue") + transactionValue);
            }
            
            countryMap.put(destination, countryData);
        }
        
        transactionsByCountry.addAll(countryMap.values());
        
        // Generate some compliance alerts
        List<Document> complianceAlerts = generateComplianceAlerts();
        
        // Populate dashboard data
        dashboardData.put("pendingTransactions", pendingTransactions);
        dashboardData.put("expiringLicenses", expiringLicenses);
        dashboardData.put("pendingScreenings", pendingScreenings);
        dashboardData.put("recentTransactions", recentTransactions);
        dashboardData.put("licenseUtilization", licenseUtilization);
        dashboardData.put("transactionsByCountry", transactionsByCountry);
        dashboardData.put("complianceAlerts", complianceAlerts);
        
        return ResponseEntity.ok(dashboardData);
    }

    @GetMapping("/licenses")
    public ResponseEntity<List<Document>> getLicenses() {
        MongoDatabase database = mongoClient.getDatabase("eccn_management");
        MongoCollection<Document> licensesCollection = database.getCollection("export_licenses");
        
        List<Document> licenses = licensesCollection.find()
                .into(new ArrayList<>())
                .stream()
                .map(this::convertIdToString)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/licenses/{id}")
    public ResponseEntity<Document> getLicenseById(@PathVariable String id) {
        MongoDatabase database = mongoClient.getDatabase("eccn_management");
        MongoCollection<Document> licensesCollection = database.getCollection("export_licenses");
        
        Document license = licensesCollection.find(new Document("_id", id))
                .first();
        
        if (license == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(convertIdToString(license));
    }

    @GetMapping("/country-restrictions")
    public ResponseEntity<List<Document>> getCountryRestrictions() {
        MongoDatabase database = mongoClient.getDatabase("eccn_management");
        MongoCollection<Document> restrictionsCollection = database.getCollection("country_restrictions");
        
        List<Document> restrictions = restrictionsCollection.find()
                .into(new ArrayList<>())
                .stream()
                .map(this::convertIdToString)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(restrictions);
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Document>> getCustomers() {
        MongoDatabase database = mongoClient.getDatabase("eccn_management");
        MongoCollection<Document> customersCollection = database.getCollection("customers");
        
        List<Document> customers = customersCollection.find()
                .into(new ArrayList<>())
                .stream()
                .map(this::convertIdToString)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Document>> getTransactions() {
        MongoDatabase database = mongoClient.getDatabase("eccn_management");
        MongoCollection<Document> transactionsCollection = database.getCollection("export_transactions");
        
        List<Document> transactions = transactionsCollection.find()
                .into(new ArrayList<>())
                .stream()
                .map(this::convertIdToString)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactions);
    }

    // Helper methods
    private Document convertIdToString(Document doc) {
        if (doc.containsKey("_id")) {
            doc.put("id", doc.getObjectId("_id").toString());
            doc.remove("_id");
        }
        return doc;
    }

    private String getCountryName(String countryCode) {
        // This would typically come from a country database or service
        Map<String, String> countryCodes = new HashMap<>();
        countryCodes.put("US", "United States");
        countryCodes.put("GB", "United Kingdom");
        countryCodes.put("CA", "Canada");
        countryCodes.put("DE", "Germany");
        countryCodes.put("FR", "France");
        countryCodes.put("JP", "Japan");
        countryCodes.put("CN", "China");
        countryCodes.put("IN", "India");
        countryCodes.put("BR", "Brazil");
        countryCodes.put("RU", "Russia");
        countryCodes.put("AE", "United Arab Emirates");
        countryCodes.put("SA", "Saudi Arabia");
        countryCodes.put("SG", "Singapore");
        countryCodes.put("AU", "Australia");
        countryCodes.put("MX", "Mexico");
        
        return countryCodes.getOrDefault(countryCode, countryCode);
    }

    private List<Document> generateComplianceAlerts() {
        List<Document> alerts = new ArrayList<>();
        
        // Add some sample alerts
        alerts.add(new Document()
                .append("id", UUID.randomUUID().toString())
                .append("alertType", "LICENSE_EXPIRATION")
                .append("severity", "HIGH")
                .append("message", "License G789012 expires in 30 days")
                .append("date", new Date())
                .append("relatedItemId", "G789012")
                .append("relatedItemType", "LICENSE")
                .append("actionRequired", true));
        
        alerts.add(new Document()
                .append("id", UUID.randomUUID().toString())
                .append("alertType", "DENIED_PARTY")
                .append("severity", "HIGH")
                .append("message", "Potential denied party match found for new customer")
                .append("date", new Date())
                .append("relatedItemId", "CUST123")
                .append("relatedItemType", "CUSTOMER")
                .append("actionRequired", true));
        
        alerts.add(new Document()
                .append("id", UUID.randomUUID().toString())
                .append("alertType", "REGULATORY_UPDATE")
                .append("severity", "MEDIUM")
                .append("message", "New export controls for encryption products to China")
                .append("date", new Date())
                .append("relatedItemId", "REG456")
                .append("relatedItemType", "REGULATION")
                .append("actionRequired", false));
        
        return alerts;
    }
}
