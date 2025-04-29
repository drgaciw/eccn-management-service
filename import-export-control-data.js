// MongoDB Import Script for ECCN Management Service
// Run with: docker exec -i eccn_mongodb mongosh -u root -p secret --authenticationDatabase admin eccn_management < import-export-control-data.js

// Clear existing collections
db.eccn_codes.drop();
db.export_controls.drop();
db.products.drop();
db.country_restrictions.drop();
db.customers.drop();
db.export_licenses.drop();
db.export_transactions.drop();

// Insert ECCN Codes
const eccnCodes = [
  {
    commodityCode: "5D002",
    description: "Information Security Software",
    category: "Encryption",
    encryptionRelated: true,
    encryptionControlNumber: "5D002",
    encryptionAlgorithms: ["AES", "RSA", "SHA-256"],
    financialSoftware: true,
    supportedPaymentMethods: ["Credit Card", "ACH", "Wire Transfer"],
    fraudDetectionCapabilities: true,
    dataAnalytics: false,
    analyticsCapabilities: [],
    applicableEARControls: ["ENC", "NS1"],
    exceptions: ["TSU", "ENC"]
  },
  {
    commodityCode: "5A002",
    description: "Information Security Systems, Equipment and Components",
    category: "Encryption",
    encryptionRelated: true,
    encryptionControlNumber: "5A002",
    encryptionAlgorithms: ["AES", "3DES", "RSA"],
    financialSoftware: false,
    supportedPaymentMethods: [],
    fraudDetectionCapabilities: false,
    dataAnalytics: false,
    analyticsCapabilities: [],
    applicableEARControls: ["ENC", "NS1"],
    exceptions: ["TSU"]
  },
  {
    commodityCode: "4D004",
    description: "Software for Intrusion and Monitoring Systems",
    category: "Security",
    encryptionRelated: false,
    encryptionControlNumber: "",
    encryptionAlgorithms: [],
    financialSoftware: false,
    supportedPaymentMethods: [],
    fraudDetectionCapabilities: true,
    dataAnalytics: true,
    analyticsCapabilities: ["Anomaly Detection", "Pattern Recognition"],
    applicableEARControls: ["NS1", "AT"],
    exceptions: []
  },
  {
    commodityCode: "3D991",
    description: "Software for Development of Microprocessors",
    category: "Electronics",
    encryptionRelated: false,
    encryptionControlNumber: "",
    encryptionAlgorithms: [],
    financialSoftware: false,
    supportedPaymentMethods: [],
    fraudDetectionCapabilities: false,
    dataAnalytics: false,
    analyticsCapabilities: [],
    applicableEARControls: ["AT"],
    exceptions: []
  },
  {
    commodityCode: "EAR99",
    description: "Items subject to the EAR but not on the Commerce Control List",
    category: "General",
    encryptionRelated: false,
    encryptionControlNumber: "",
    encryptionAlgorithms: [],
    financialSoftware: false,
    supportedPaymentMethods: [],
    fraudDetectionCapabilities: false,
    dataAnalytics: false,
    analyticsCapabilities: [],
    applicableEARControls: [],
    exceptions: []
  }
];

db.eccn_codes.insertMany(eccnCodes);
print("Inserted ECCN Codes: " + db.eccn_codes.countDocuments());

// Insert Products
const products = [
  {
    name: "Payment Gateway",
    description: "Secure payment processing solution",
    features: ["Credit Card Processing", "Fraud Detection", "Encryption", "Tokenization"],
    versions: [
      {
        versionNumber: "1.0.0",
        releaseDate: "2023-01-15",
        encryptionLibraries: ["OpenSSL", "BouncyCastle"],
        classificationStatus: "CLASSIFIED"
      },
      {
        versionNumber: "1.1.0",
        releaseDate: "2023-06-20",
        encryptionLibraries: ["OpenSSL", "BouncyCastle", "NaCl"],
        classificationStatus: "PENDING"
      }
    ],
    status: "ACTIVE",
    usContentPercentage: 85.5,
    militaryUse: false,
    controlledCountry: false,
    encryptionEnabled: true
  },
  {
    name: "Fraud Detection System",
    description: "AI-powered fraud detection for financial transactions",
    features: ["Machine Learning", "Real-time Analysis", "Pattern Recognition"],
    versions: [
      {
        versionNumber: "2.3.1",
        releaseDate: "2023-03-10",
        encryptionLibraries: ["OpenSSL"],
        classificationStatus: "CLASSIFIED"
      }
    ],
    status: "ACTIVE",
    usContentPercentage: 92.0,
    militaryUse: false,
    controlledCountry: false,
    encryptionEnabled: true
  },
  {
    name: "Transaction Monitoring",
    description: "Real-time transaction monitoring and alerting",
    features: ["Real-time Monitoring", "Compliance Checks", "Reporting"],
    versions: [
      {
        versionNumber: "3.1.0",
        releaseDate: "2023-02-05",
        encryptionLibraries: [],
        classificationStatus: "CLASSIFIED"
      }
    ],
    status: "ACTIVE",
    usContentPercentage: 78.0,
    militaryUse: false,
    controlledCountry: false,
    encryptionEnabled: false
  },
  {
    name: "Secure Messaging",
    description: "End-to-end encrypted messaging platform",
    features: ["End-to-End Encryption", "Secure File Transfer", "Message Expiration"],
    versions: [
      {
        versionNumber: "1.5.2",
        releaseDate: "2023-04-18",
        encryptionLibraries: ["Signal Protocol", "OpenSSL"],
        classificationStatus: "CLASSIFIED"
      }
    ],
    status: "ACTIVE",
    usContentPercentage: 95.0,
    militaryUse: false,
    controlledCountry: false,
    encryptionEnabled: true
  }
];

db.products.insertMany(products);
print("Inserted Products: " + db.products.countDocuments());

// Insert Export Controls
const exportControls = [
  {
    moduleName: "Payment Gateway Core",
    earClassification: "5D002",
    jurisdictionClassifications: {
      "EU": "5D002",
      "UK": "5D002",
      "JP": "5D002",
      "CA": "5D002"
    },
    conflictingJurisdictions: [],
    unifiedClassification: "5D002",
    complianceRequirements: ["BIS Notification", "Annual Self-Classification Report"],
    requiresSpecialHandling: false
  },
  {
    moduleName: "Encryption Module",
    earClassification: "5D002",
    jurisdictionClassifications: {
      "EU": "5D002",
      "UK": "5D002",
      "JP": "5D002",
      "CA": "5D002"
    },
    conflictingJurisdictions: [],
    unifiedClassification: "5D002",
    complianceRequirements: ["BIS Notification", "Annual Self-Classification Report", "TSU Exception Documentation"],
    requiresSpecialHandling: true
  },
  {
    moduleName: "Fraud Detection Engine",
    earClassification: "4D004",
    jurisdictionClassifications: {
      "EU": "4D004",
      "UK": "ML21",
      "JP": "4D004",
      "CA": "4D004"
    },
    conflictingJurisdictions: ["UK"],
    unifiedClassification: "4D004",
    complianceRequirements: ["End-Use Verification"],
    requiresSpecialHandling: false
  },
  {
    moduleName: "Transaction Processing",
    earClassification: "EAR99",
    jurisdictionClassifications: {
      "EU": "N/A",
      "UK": "N/A",
      "JP": "N/A",
      "CA": "N/A"
    },
    conflictingJurisdictions: [],
    unifiedClassification: "EAR99",
    complianceRequirements: [],
    requiresSpecialHandling: false
  }
];

db.export_controls.insertMany(exportControls);
print("Inserted Export Controls: " + db.export_controls.countDocuments());

// Insert Country Restrictions
const countryRestrictions = [
  {
    countryCode: "CN",
    countryName: "China",
    restrictionLevel: "RESTRICTED",
    requiresLicense: true,
    embargoType: null,
    notes: "Requires license for encryption items",
    lastUpdated: new Date()
  },
  {
    countryCode: "RU",
    countryName: "Russia",
    restrictionLevel: "HIGHLY_RESTRICTED",
    requiresLicense: true,
    embargoType: "Partial",
    notes: "Significant restrictions due to sanctions",
    lastUpdated: new Date()
  },
  {
    countryCode: "IR",
    countryName: "Iran",
    restrictionLevel: "PROHIBITED",
    requiresLicense: true,
    embargoType: "Full Embargo",
    notes: "Comprehensive sanctions in place",
    lastUpdated: new Date()
  },
  {
    countryCode: "CU",
    countryName: "Cuba",
    restrictionLevel: "PROHIBITED",
    requiresLicense: true,
    embargoType: "Full Embargo",
    notes: "Comprehensive sanctions in place",
    lastUpdated: new Date()
  },
  {
    countryCode: "KP",
    countryName: "North Korea",
    restrictionLevel: "PROHIBITED",
    requiresLicense: true,
    embargoType: "Full Embargo",
    notes: "Comprehensive sanctions in place",
    lastUpdated: new Date()
  },
  {
    countryCode: "SY",
    countryName: "Syria",
    restrictionLevel: "PROHIBITED",
    requiresLicense: true,
    embargoType: "Full Embargo",
    notes: "Comprehensive sanctions in place",
    lastUpdated: new Date()
  },
  {
    countryCode: "CA",
    countryName: "Canada",
    restrictionLevel: "OPEN",
    requiresLicense: false,
    embargoType: null,
    notes: null,
    lastUpdated: new Date()
  },
  {
    countryCode: "GB",
    countryName: "United Kingdom",
    restrictionLevel: "OPEN",
    requiresLicense: false,
    embargoType: null,
    notes: null,
    lastUpdated: new Date()
  },
  {
    countryCode: "JP",
    countryName: "Japan",
    restrictionLevel: "OPEN",
    requiresLicense: false,
    embargoType: null,
    notes: null,
    lastUpdated: new Date()
  },
  {
    countryCode: "DE",
    countryName: "Germany",
    restrictionLevel: "OPEN",
    requiresLicense: false,
    embargoType: null,
    notes: null,
    lastUpdated: new Date()
  },
  {
    countryCode: "FR",
    countryName: "France",
    restrictionLevel: "OPEN",
    requiresLicense: false,
    embargoType: null,
    notes: null,
    lastUpdated: new Date()
  },
  {
    countryCode: "IN",
    countryName: "India",
    restrictionLevel: "CONTROLLED",
    requiresLicense: true,
    embargoType: null,
    notes: "License required for certain encryption items",
    lastUpdated: new Date()
  },
  {
    countryCode: "BR",
    countryName: "Brazil",
    restrictionLevel: "CONTROLLED",
    requiresLicense: true,
    embargoType: null,
    notes: "License required for certain encryption items",
    lastUpdated: new Date()
  },
  {
    countryCode: "AE",
    countryName: "United Arab Emirates",
    restrictionLevel: "CONTROLLED",
    requiresLicense: true,
    embargoType: null,
    notes: "License required for certain encryption items",
    lastUpdated: new Date()
  },
  {
    countryCode: "SA",
    countryName: "Saudi Arabia",
    restrictionLevel: "CONTROLLED",
    requiresLicense: true,
    embargoType: null,
    notes: "License required for certain encryption items",
    lastUpdated: new Date()
  }
];

db.country_restrictions.insertMany(countryRestrictions);
print("Inserted Country Restrictions: " + db.country_restrictions.countDocuments());

// Insert Customers
const customers = [
  {
    name: "Global Bank Corp",
    address: "123 Finance St, New York, NY 10001",
    country: "US",
    screeningStatus: "APPROVED",
    lastScreenedDate: new Date(),
    endUseStatement: "Financial transaction processing for retail banking",
    endUserCertification: true,
    redFlagIndicators: []
  },
  {
    name: "European Payments Ltd",
    address: "45 Banking Lane, London, EC2V 8AE",
    country: "GB",
    screeningStatus: "APPROVED",
    lastScreenedDate: new Date(),
    endUseStatement: "Payment processing for e-commerce platforms",
    endUserCertification: true,
    redFlagIndicators: []
  },
  {
    name: "Asia Tech Solutions",
    address: "78 Technology Park, Singapore 138632",
    country: "SG",
    screeningStatus: "APPROVED",
    lastScreenedDate: new Date(),
    endUseStatement: "Integration with banking systems for payment processing",
    endUserCertification: true,
    redFlagIndicators: []
  },
  {
    name: "Dubai Financial Services",
    address: "Dubai International Financial Centre, Dubai",
    country: "AE",
    screeningStatus: "REQUIRES_REVIEW",
    lastScreenedDate: new Date(),
    endUseStatement: "Financial transaction processing for corporate clients",
    endUserCertification: true,
    redFlagIndicators: [
      {
        description: "Unclear end-use statement",
        severity: "LOW",
        mitigated: false,
        mitigationNotes: null
      }
    ]
  },
  {
    name: "Brazilian Payments Inc",
    address: "Av. Paulista, 1000, São Paulo",
    country: "BR",
    screeningStatus: "APPROVED",
    lastScreenedDate: new Date(),
    endUseStatement: "Payment processing for retail and e-commerce",
    endUserCertification: true,
    redFlagIndicators: []
  }
];

db.customers.insertMany(customers);
print("Inserted Customers: " + db.customers.countDocuments());

// Insert Export Licenses
const exportLicenses = [
  {
    licenseNumber: "D123456",
    licenseType: "INDIVIDUAL",
    issueDate: new Date("2023-01-15"),
    expirationDate: new Date("2025-01-14"),
    issuingAuthority: "Bureau of Industry and Security",
    products: ["Payment Gateway", "Encryption Module"],
    countries: ["CN", "RU"],
    valueLimit: 1000000,
    valueUsed: 250000,
    status: "ACTIVE",
    notes: "License for encryption products to restricted countries"
  },
  {
    licenseNumber: "G789012",
    licenseType: "GLOBAL",
    issueDate: new Date("2023-03-10"),
    expirationDate: new Date("2026-03-09"),
    issuingAuthority: "Bureau of Industry and Security",
    products: ["Payment Gateway", "Fraud Detection System", "Transaction Monitoring"],
    countries: ["IN", "BR", "AE", "SA"],
    valueLimit: 5000000,
    valueUsed: 1200000,
    status: "ACTIVE",
    notes: "Global license for controlled countries"
  },
  {
    licenseNumber: "E345678",
    licenseType: "EXCEPTION",
    issueDate: new Date("2023-02-20"),
    expirationDate: new Date("2024-02-19"),
    issuingAuthority: "Bureau of Industry and Security",
    products: ["Secure Messaging"],
    countries: ["CA", "GB", "JP", "DE", "FR"],
    valueLimit: null,
    valueUsed: null,
    status: "ACTIVE",
    notes: "License exception for encryption items to close allies"
  }
];

db.export_licenses.insertMany(exportLicenses);
print("Inserted Export Licenses: " + db.export_licenses.countDocuments());

// Insert Export Transactions
const exportTransactions = [
  {
    transactionDate: new Date("2023-05-15"),
    customer: customers[0],
    destination: "US",
    products: [
      {
        productId: "Payment Gateway",
        productName: "Payment Gateway",
        eccn: "5D002",
        quantity: 1,
        value: 75000,
        controlReason: "Encryption"
      }
    ],
    license: null,
    licenseException: "NLR",
    screeningResult: {
      status: "APPROVED",
      screenedDate: new Date("2023-05-10"),
      screenedBy: "John Smith",
      listMatches: [],
      redFlagIndicators: [],
      notes: "No issues found"
    },
    documentationStatus: "COMPLETE",
    status: "COMPLETED",
    totalValue: 75000,
    shipmentDate: new Date("2023-05-20"),
    notes: "Standard transaction to domestic customer",
    createdBy: "admin",
    createdDate: new Date("2023-05-01"),
    lastModifiedBy: "admin",
    lastModifiedDate: new Date("2023-05-20")
  },
  {
    transactionDate: new Date("2023-06-10"),
    customer: customers[1],
    destination: "GB",
    products: [
      {
        productId: "Payment Gateway",
        productName: "Payment Gateway",
        eccn: "5D002",
        quantity: 1,
        value: 80000,
        controlReason: "Encryption"
      },
      {
        productId: "Fraud Detection System",
        productName: "Fraud Detection System",
        eccn: "4D004",
        quantity: 1,
        value: 65000,
        controlReason: "Security Software"
      }
    ],
    license: exportLicenses[2].licenseNumber,
    licenseException: null,
    screeningResult: {
      status: "APPROVED",
      screenedDate: new Date("2023-06-05"),
      screenedBy: "Jane Doe",
      listMatches: [],
      redFlagIndicators: [],
      notes: "No issues found"
    },
    documentationStatus: "COMPLETE",
    status: "COMPLETED",
    totalValue: 145000,
    shipmentDate: new Date("2023-06-15"),
    notes: "Transaction to UK customer",
    createdBy: "admin",
    createdDate: new Date("2023-06-01"),
    lastModifiedBy: "admin",
    lastModifiedDate: new Date("2023-06-15")
  },
  {
    transactionDate: new Date("2023-07-20"),
    customer: customers[3],
    destination: "AE",
    products: [
      {
        productId: "Payment Gateway",
        productName: "Payment Gateway",
        eccn: "5D002",
        quantity: 1,
        value: 90000,
        controlReason: "Encryption"
      }
    ],
    license: exportLicenses[1].licenseNumber,
    licenseException: null,
    screeningResult: {
      status: "REQUIRES_REVIEW",
      screenedDate: new Date("2023-07-15"),
      screenedBy: "Robert Johnson",
      listMatches: [],
      redFlagIndicators: [
        {
          description: "Unclear end-use statement",
          severity: "LOW",
          mitigated: false,
          mitigationNotes: null
        }
      ],
      notes: "Additional end-use verification required"
    },
    documentationStatus: "IN_PROGRESS",
    status: "PENDING_APPROVAL",
    totalValue: 90000,
    shipmentDate: null,
    notes: "Pending additional end-use verification",
    createdBy: "admin",
    createdDate: new Date("2023-07-10"),
    lastModifiedBy: "admin",
    lastModifiedDate: new Date("2023-07-15")
  },
  {
    transactionDate: new Date("2023-08-05"),
    customer: customers[2],
    destination: "SG",
    products: [
      {
        productId: "Secure Messaging",
        productName: "Secure Messaging",
        eccn: "5D002",
        quantity: 1,
        value: 55000,
        controlReason: "Encryption"
      }
    ],
    license: null,
    licenseException: "ENC",
    screeningResult: {
      status: "APPROVED",
      screenedDate: new Date("2023-08-01"),
      screenedBy: "Sarah Williams",
      listMatches: [],
      redFlagIndicators: [],
      notes: "No issues found"
    },
    documentationStatus: "COMPLETE",
    status: "SHIPPED",
    totalValue: 55000,
    shipmentDate: new Date("2023-08-10"),
    notes: "Transaction to Singapore customer",
    createdBy: "admin",
    createdDate: new Date("2023-07-25"),
    lastModifiedBy: "admin",
    lastModifiedDate: new Date("2023-08-10")
  },
  {
    transactionDate: new Date("2023-09-15"),
    customer: customers[4],
    destination: "BR",
    products: [
      {
        productId: "Payment Gateway",
        productName: "Payment Gateway",
        eccn: "5D002",
        quantity: 1,
        value: 85000,
        controlReason: "Encryption"
      },
      {
        productId: "Transaction Monitoring",
        productName: "Transaction Monitoring",
        eccn: "EAR99",
        quantity: 1,
        value: 45000,
        controlReason: null
      }
    ],
    license: exportLicenses[1].licenseNumber,
    licenseException: null,
    screeningResult: {
      status: "APPROVED",
      screenedDate: new Date("2023-09-10"),
      screenedBy: "Michael Brown",
      listMatches: [],
      redFlagIndicators: [],
      notes: "No issues found"
    },
    documentationStatus: "COMPLETE",
    status: "APPROVED",
    totalValue: 130000,
    shipmentDate: null,
    notes: "Approved, awaiting shipment",
    createdBy: "admin",
    createdDate: new Date("2023-09-01"),
    lastModifiedBy: "admin",
    lastModifiedDate: new Date("2023-09-12")
  }
];

db.export_transactions.insertMany(exportTransactions);
print("Inserted Export Transactions: " + db.export_transactions.countDocuments());

print("Data import completed successfully!");
