# ECCN Management Service Documentation

## Service Classes and Methods

### CryptoClassificationService
**Description**: Handles the classification of cryptographic algorithms and implementations based on ECCN regulations.

**Methods**:
- `classifyCryptography(int keyLength, Algorithm algorithm, boolean isMassMarket)`: Classifies cryptographic implementations based on key length, algorithm, and mass market status.
- `classifyCryptoAlgorithm(Algorithm algorithm, int keyLength, Mode mode)`: Classifies cryptographic algorithms based on key length and mode.
- `classifyCryptoImplementation(String sourceCode, String language)`: Classifies cryptographic implementations based on source code and programming language.
- `assessDeMinimis(Product product)`: Determines if a product qualifies for de minimis treatment under ECCN regulations.
- `getEncryptionRegistrationNumber(Product product)`: Generates an encryption registration number for products requiring ERN.
- `analyzeCryptoLibrary(String library, String version)`: Analyzes a cryptographic library for ECCN compliance.
- `qualifiesForMassMarket(int keyLength, Algorithm algorithm)`: Determines if a cryptographic implementation qualifies for mass market treatment.
- `qualifiesForControlledEncryption(int keyLength, Algorithm algorithm)`: Determines if a cryptographic implementation qualifies as controlled encryption.
- `isRestrictedAlgorithm(Algorithm algorithm)`: Checks if an algorithm is restricted under ECCN regulations.
- `isWeakMode(Mode mode)`: Checks if a cryptographic mode is considered weak under ECCN regulations.

### AutomatedClassificationToolService
**Description**: Automates the classification of software modules based on cryptographic libraries and payment processing capabilities.

**Methods**:
- `analyzeSourceCode(String repositoryUrl, String branch, String commit)`: Analyzes source code for cryptographic libraries and payment processing.
- `analyzeSoftwarePackage(String packageName, String packageType)`: Analyzes software packages for cryptographic libraries and payment processing.
- `validateClassification(String moduleName, String proposedECCN)`: Validates a proposed ECCN classification against the module's analysis.
- `suggestECCN(String moduleName)`: Suggests possible ECCN classifications for a module.
- `generateClassificationReport(String moduleName)`: Generates a detailed classification report for a module.
- `integrateAIModel(AIModel model)`: Integrates an AI model for enhanced classification suggestions.
- `determineClassification(List<String> encryptionLibraries, boolean hasPaymentProcessing)`: Determines the ECCN classification based on encryption libraries and payment processing.

### EccnService
**Description**: Manages ECCN classifications and related operations.

**Methods**:
- `getEccnClassification(String productId)`: Retrieves the ECCN classification for a product.
- `updateEccnClassification(String productId, String eccn)`: Updates the ECCN classification for a product.
- `validateEccnClassification(String eccn)`: Validates an ECCN classification against regulatory standards.

### DocumentRecordService
**Description**: Manages document records related to ECCN classifications.

**Methods**:
- `createDocumentRecord(DocumentRecord document)`: Creates a new document record.
- `getDocumentRecord(String documentId)`: Retrieves a document record by ID.
- `updateDocumentRecord(DocumentRecord document)`: Updates an existing document record.
- `deleteDocumentRecord(String documentId)`: Deletes a document record.

### ExportControlService
**Description**: Manages export control regulations and compliance.

**Methods**:
- `checkExportCompliance(String productId, String destinationCountry)`: Checks if a product is compliant with export regulations for a destination country.
- `getExportControlRegulations(String country)`: Retrieves export control regulations for a specific country.
- `validateExportLicense(String licenseId)`: Validates an export license.

### RiskAssessmentService
**Description**: Assesses risks related to ECCN classifications and export controls.

**Methods**:
- `assessRisk(String productId)`: Assesses the risk associated with a product's ECCN classification.
- `generateRiskReport(String productId)`: Generates a detailed risk assessment report for a product.
- `mitigateRisk(String productId, String mitigationStrategy)`: Applies a risk mitigation strategy to a product.

### ProductService
**Description**: Manages product-related operations and ECCN classifications.

**Methods**:
- `createProduct(Product product)`: Creates a new product record.
- `getProduct(String productId)`: Retrieves a product by ID.
- `updateProduct(Product product)`: Updates an existing product record.
- `deleteProduct(String productId)`: Deletes a product record.
- `classifyProduct(String productId)`: Classifies a product based on its attributes and ECCN regulations.

### EccnClassificationWorkflowService
**Description**: Manages the workflow for ECCN classification processes.

**Methods**:
- `startClassificationWorkflow(String productId)`: Starts a new ECCN classification workflow for a product.
- `getWorkflowStatus(String workflowId)`: Retrieves the status of a classification workflow.
- `completeWorkflow(String workflowId)`: Completes a classification workflow.
- `escalateWorkflow(String workflowId, String reason)`: Escalates a classification workflow for further review.

### AutomatedClassificationToolService.AIModel
**Description**: Interface for AI models used in automated classification.

**Methods**:
- `classify(List<String> encryptionLibraries, boolean hasPaymentProcessing)`: Classifies a module based on encryption libraries and payment processing.
- `suggestClassifications(List<String> encryptionLibraries, boolean hasPaymentProcessing)`: Suggests possible ECCN classifications for a module.