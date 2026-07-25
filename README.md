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
- **Backend**: Spring Boot 4.0.7
- **Database**: MongoDB
- **API Documentation**: Springdoc OpenAPI
- **Testing**: JUnit 5, Mockito, Testcontainers
- **Containerization**: Docker
- **Monitoring**: Spring Boot Actuator

## Getting Started

### Prerequisites
- Java 21
- MongoDB 7.0+
- Docker (optional)

> The project bundles a Maven wrapper (`./mvnw`), so a local Maven install is optional.

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
   ./mvnw clean install
   ```
4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Docker Setup
```bash
docker compose up -d
```

## Configuration
MongoDB connection and admin credentials are externalized via environment variables, each with a safe local default:

| Variable | Description | Local default |
|----------|-------------|---------------|
| `SPRING_DATA_MONGODB_URI` | MongoDB connection URI | `mongodb://localhost:27017/eccn_management` |
| `MONGODB_USERNAME` | MongoDB username | `root` |
| `MONGODB_PASSWORD` | MongoDB password | `secret` |
| `SECURITY_ADMIN_NAME` | HTTP Basic admin username | `admin` |
| `SECURITY_ADMIN_PASSWORD` | HTTP Basic admin password | `admin` (noop-encoded) |

## Build & Test
```bash
./mvnw clean verify
```

## API Documentation
The API documentation is available at `http://localhost:8080/swagger-ui.html` when the application is running. Use the **Authorize** button to authenticate with HTTP Basic credentials.

## Security
The API is secured with HTTP Basic authentication (in-memory user). JWT/OIDC support is a planned future addition.

## Contribution Guidelines
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.