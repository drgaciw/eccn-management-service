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
4. Run the application (foreground):
   ```bash
   ./mvnw spring-boot:run
   ```
   Or use the **detached local restart** script (recommended for IDE/agent shells that
   otherwise SIGTERM attached `spring-boot:run` processes):
   ```bash
   ./scripts/restart-local.sh          # stop + start on :8081
   ./scripts/restart-local.sh status
   ./scripts/restart-local.sh stop
   ```
   Defaults: HTTP Basic `devuser` / `devpass123`, log at `.local-backend.log`.

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
| `SECURITY_ADMIN_PASSWORD` | HTTP Basic admin password | `{noop}admin` (delegating encoder) |
| `SERVER_PORT` | HTTP port | `8081` |

## Build & Test
```bash
./mvnw clean verify
```

## API Documentation
The API documentation is available at `http://localhost:8081/swagger-ui.html` when the application is running. Use the **Authorize** button to authenticate with HTTP Basic credentials.

## Security
The API is secured with HTTP Basic authentication (in-memory user) using a
`DelegatingPasswordEncoder` (`{noop}`, `{bcrypt}`, …). JWT/OIDC support is a planned future addition.

## Contribution Guidelines
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.