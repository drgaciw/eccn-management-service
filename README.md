# ECCN Management Service

## Overview
The ECCN Management Service is a comprehensive solution for managing and tracking Export Control Classification Numbers (ECCN) in compliance with international trade regulations. This service provides tools for classification, documentation, risk assessment, and compliance monitoring.

## Key Features
- ECCN classification workflow management
- Automated classification tool integration
- Crypto classification capabilities
- Document version control and tracking
- Risk assessment and management
- Export control compliance monitoring
- Product portfolio management

## Technology Stack
- **Backend**: Spring Boot 3.x
- **Database**: MongoDB
- **API Documentation**: Springdoc OpenAPI
- **Testing**: JUnit 5, MockMvc
- **Containerization**: Docker
- **CI/CD**: GitHub Actions
- **Monitoring**: Spring Boot Actuator

## Getting Started

### Prerequisites
- Java 17 or later
- Maven 3.8+
- MongoDB 6.0+
- Docker (optional)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/eccn-management-service.git
   ```
2. Navigate to the project directory:
   ```bash
   cd eccn-management-service
   ```
3. Build the project:
   ```bash
   mvn clean install
   ```
4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Docker Setup
```bash
docker-compose up -d
```

## API Documentation
The API documentation is available at `http://localhost:8080/swagger-ui.html` when the application is running.

## Contribution Guidelines
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.