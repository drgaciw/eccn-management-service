# ECCN Management Service — Technology Stack

## Document Metadata

- **Repository**: `eccn-management-service`
- **Generated**: 2026-07-08
- **Method**: Reverse-engineered from `pom.xml`, Maven wrapper, source annotations, resources, tests, Docker/Compose, Postman assets, scripts, README, and subagent findings.
- **Note on requested tooling**: No `sequential-thinking` MCP tool was exposed in this session; this document was produced using an explicit sequential analysis workflow plus read-only subagents.

---

## Executive Summary

The project is a Java 21 Spring Boot 3.4.1 service for ECCN/export-control management. It uses Maven, Spring MVC REST controllers, Spring Data MongoDB repositories, Spring Security defaults, Springdoc OpenAPI, Actuator, Spring Cloud LoadBalancer, Resilience4j, Lombok, and a MongoDB datastore. It includes Docker/Compose local deployment, Postman/Newman API testing assets, and Python-based Java complexity tooling.

The implementation and documentation are not fully aligned: README says Java 17+ while the build and Dockerfile use Java 21; README claims GitHub Actions but no workflow is present; Postman describes multiple services and Keycloak-like auth that are not provided by Compose; several Spring dependencies are present but only partially used.

---

## Stack at a Glance

| Layer | Technology | Version / Evidence | Status |
|---|---|---:|---|
| Language | Java | 21 via `pom.xml` `java.version`; Docker uses Temurin 21 | Active |
| Build | Maven Wrapper | Wrapper 3.3.2, Maven 3.9.9 | Active |
| Framework | Spring Boot | 4.0.7 parent | Active |
| Cloud BOM | Spring Cloud | 2025.1.2 | Active dependency management |
| API | Spring Web MVC REST | `@RestController`, `@RequestMapping` controllers | Active |
| Reactive Web | Spring WebFlux | Dependency present | No main-code usage found |
| Persistence | Spring Data MongoDB | `MongoRepository`, `@Document` | Active |
| Database | MongoDB | Local properties; Compose and tests Mongo 7.0 | Active |
| Security | Spring Security | Starter + default `admin/admin` user | Present, incomplete custom policy |
| API Docs | Springdoc OpenAPI | 3.0.3 | Active |
| Observability | Spring Boot Actuator | Starter + exposed endpoints config | Active, needs hardening |
| Resilience | Resilience4j / Spring Cloud CircuitBreaker | Resilience4j 2.1.0, one `@CircuitBreaker` | Partially used |
| Load Balancing | Spring Cloud LoadBalancer | Dependency + tests | Partially used |
| Cache | Spring Cache + Caffeine | Caffeine 3.1.0, `@EnableCaching` | Disabled at runtime |
| Async | Spring Async | `@EnableAsync`, executor config | Configured; no `@Async` found |
| Codegen | Lombok | 1.18.30 | Active, compile-risk area |
| Testing | JUnit 5, Spring Boot Test, Mockito, Reactor Test, Testcontainers | POM + tests | Active |
| Container | Docker | `eclipse-temurin:21-jdk-jammy` | Active |
| Local orchestration | Docker Compose | App + MongoDB | Active |
| API testing | Postman + Newman | Collection + shell runner | Present, partially mismatched |
| Static analysis tooling | Python cyclomatic complexity script | `scripts/analyze_complexity.py` | Present |
| IDE tooling | VS Code, Eclipse metadata | `.vscode`, `.project`, `.classpath`, `.settings` | Present |

---

## System Context

```mermaid
flowchart LR
    Dev[Developer]
    UI[Web UI / API Client]
    Postman[Postman / Newman]
    Service["ECCN Management Service\nJava 21 + Spring Boot 4.0.7"]
    Mongo[(MongoDB)]
    Swagger[Swagger UI / OpenAPI]
    FutureAuth["Future Auth Provider\nKeycloak/OIDC implied by Postman"]
    FutureSystems["Future Enterprise Systems\nPDH / Salesforce / External ECCN DB"]

    Dev -->|Maven Wrapper / IDE| Service
    UI -->|REST / JSON| Service
    Postman -->|API tests| Service
    Service --> Mongo
    Service --> Swagger
    Service -. target / not implemented .-> FutureAuth
    Service -. interfaces / stubs .-> FutureSystems
```

### Rationale

- The current concrete runtime is one Spring Boot API plus MongoDB.
- Postman implies a broader multi-service target with auth and separate classification/risk/compliance services.
- Enterprise integrations exist as interfaces/stubs rather than concrete adapters.

---

## Container / Component View

```mermaid
flowchart TB
    subgraph Runtime[Spring Boot Runtime]
        App[EccnManagementServiceApplication\n@SpringBootApplication]
        Controllers[REST Controllers\nECCN / Product / Glossary / Crypto / Health]
        Services[Domain Services\nClassification / Documents / Risk / Export Controls / Workflow]
        Repos[Spring Data Mongo Repositories]
        Config[Config\nOpenAPI / CORS / Mongo Auditing]
        Exceptions[Global Exception Handling]
    end

    subgraph Libraries[Core Libraries]
        MVC[Spring Web MVC]
        MongoLib[Spring Data MongoDB]
        Security[Spring Security]
        Actuator[Spring Boot Actuator]
        OpenAPI[Springdoc OpenAPI]
        Resilience[Spring Cloud CircuitBreaker + Resilience4j]
        Lombok[Lombok]
    end

    Mongo[(MongoDB)]

    App --> Controllers
    Controllers --> Services
    Services --> Repos
    Repos --> Mongo
    Controllers --> Exceptions
    Config --> OpenAPI
    Runtime --> Libraries
```

### Rationale

- Controllers are thin REST adapters.
- Services hold business rules and workflow logic.
- Repositories are Spring Data MongoDB interfaces.
- Several included libraries are present but not fully used or configured.

---

## Runtime Data Flow

```mermaid
flowchart LR
    Client[HTTP Client]
    Controller[Spring MVC Controller]
    Service[Service Layer]
    Validation[Validation / Business Rules]
    Repository[MongoRepository]
    Database[(MongoDB)]
    ErrorHandler[GlobalExceptionHandler]
    Response[HTTP Response]

    Client --> Controller
    Controller --> Service
    Service --> Validation
    Validation --> Repository
    Repository --> Database
    Database --> Repository
    Repository --> Service
    Service --> Response
    Validation -->|exceptions| ErrorHandler
    ErrorHandler --> Response
    Response --> Client
```

---

## Build and Language Stack

### Java

- **Java version**: 21.
- **Evidence**: `pom.xml` sets `<java.version>21</java.version>` and `maven-compiler-plugin` uses this value for `source` and `target`.
- **Container alignment**: Docker uses `eclipse-temurin:21-jdk-jammy`.
- **Documentation mismatch**: README says Java 17 or later; practical baseline is Java 21.

### Maven

- **Build tool**: Maven via Maven Wrapper.
- **Wrapper version**: 3.3.2.
- **Maven distribution**: Apache Maven 3.9.9.
- **Commands documented**: `mvn clean install`, `mvn spring-boot:run`; repository also includes `./mvnw` and `mvnw.cmd`.

### Maven Coordinates

- **Group ID**: `com.aciworldwide`
- **Artifact ID**: `eccn-management-service`
- **Version**: `0.0.1-SNAPSHOT`
- **Spring Boot parent**: `org.springframework.boot:spring-boot-starter-parent:4.0.7`

### Build Plugins

| Plugin | Version | Purpose |
|---|---:|---|
| `spring-boot-maven-plugin` | inherited | Build executable Spring Boot jar; excludes Lombok from final package |
| `maven-compiler-plugin` | 3.11.0 | Java 21 source/target compilation |
| `maven-surefire-plugin` | inherited | Test execution; configured with Byte Buddy Java agent |

### Key Build Risks

- Docker image build runs `./mvnw clean package -DskipTests`, so container builds can succeed without validating tests.
- Subagent validation reported `./mvnw -q -DskipTests compile` failures in the current workspace, mainly around missing generated accessors/builders and a repository method mismatch. Treat the current stack documentation as static reverse-engineering, not proof of a healthy build.

---

## Spring Boot Application Stack

### Core Spring Capabilities

The main application class enables:

- `@SpringBootApplication`
- Mongo repositories via `@EnableMongoRepositories`
- Mongo auditing via `@EnableMongoAuditing`
- Caching via `@EnableCaching`
- Async execution via `@EnableAsync`
- Configuration properties via `@EnableConfigurationProperties`

### REST API Layer

Implemented controllers:

| Controller | Base Path | Purpose |
|---|---|---|
| `EccnController` | `/api/v1/eccn` | ECCN CRUD, filtering, search |
| `ProductController` | `/api/products` | Product portfolio and version classification status |
| `GlossaryController` | `/api/v1/glossary` | Compliance glossary CRUD/search/bulk import |
| `CryptoClassificationController` | `/api/crypto-classification` | Crypto classification helper |
| `HealthController` | `/api/health` | Simple custom health response |

### API Documentation

- **Library**: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3`
- **Swagger UI path**: `/swagger-ui.html`
- **OpenAPI JSON path**: `/v3/api-docs`
- **Configured package scan**: `com.aciworldwide.eccn_management_service`
- **Risk**: `OpenApiConfig` adds server URL `/api` while controllers already use `/api/...`, which may make generated URL resolution confusing.

### CORS

`WebConfig` allows:

- Paths: `/api/**`
- Origins: `http://localhost:3000`, `http://eccn-management-ui:3000`
- Methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- Headers: all
- Credentials: allowed

Risk: `ProductController` uses `PATCH`, but CORS methods do not include `PATCH`.

---

## Persistence and Data Stack

### Database

- **Database**: MongoDB.
- **Local app URI**: `mongodb://localhost:27017/eccn_management`.
- **Local credentials in properties**: `root` / `secret`.
- **Compose database image**: `mongo:7.0`.
- **Testcontainers image**: `mongo:7.0` in test config.

### Spring Data MongoDB

Repositories extend `MongoRepository` and use derived queries plus selected `@Query` annotations.

| Model | Collection | Repository |
|---|---|---|
| `Eccn` | `eccns` | `EccnRepository` |
| `Product` | `products` | `ProductRepository` |
| `GlossaryEntry` | `glossary_entries` | `GlossaryEntryRepository` |
| `DocumentRecord` | `document_records` | `DocumentRecordRepository` |
| `DocumentVersion` | default collection | `DocumentVersionRepository` |
| `ExportControl` | `export_controls` | `ExportControlRepository` |
| `RiskAssessment` | `risk_assessments` | `RiskAssessmentRepository` |
| `AutomatedClassificationToolService.ModuleAnalysis` | implicit / unclear | `ClassificationHistoryRepository` |

### Persistence Risks

- No migration framework was found.
- No explicit indexes or unique database constraints were found.
- Some duplicate checks are service-level only and can race.
- `ModuleAnalysis` appears to be persisted through a repository but is an inner class without obvious `@Document`/`@Id` annotations.
- `@Transactional` is used with MongoDB; real multi-document transaction behavior requires Mongo replica set/session support.

---

## Security Stack

### Present

- `spring-boot-starter-security` is in `pom.xml`.
- `application.properties` configures a default Spring Security user:
  - username: `admin`
  - password: `admin`
  - role: `USER`
- Postman collection uses bearer-token headers and a Keycloak-like password grant flow.

### Not Found in Main Source

No explicit usage was found for:

- `SecurityFilterChain`
- `@EnableWebSecurity`
- `@PreAuthorize`
- `@Secured`
- `UserDetailsService`
- `PasswordEncoder`
- JWT resource server configuration
- OAuth2/OIDC client or resource-server configuration

### Security Risks

- Default credentials are committed in local properties.
- MongoDB credentials are committed in properties and Compose.
- Postman has `admin/admin123` auth variables.
- Actuator exposure is broad.
- No endpoint authorization matrix is defined in code.
- No TLS/mTLS/network policy configuration was found.

---

## Observability and Operations Stack

### Actuator

- **Dependency**: `spring-boot-starter-actuator`.
- **Config**: `management.endpoints.web.exposure.include=*,hystrix.stream`.
- **Health details**: `management.endpoint.health.show-details=always`.

Risk: exposing all actuator endpoints and full health details is not production-safe unless protected.

### Logging

`application.properties` configures:

- root level: `INFO`
- `com.aciworldwide`: `DEBUG`
- console pattern: timestamp, logger, message

Not found:

- JSON structured logging
- correlation IDs
- distributed tracing configuration
- metrics backend/exporter configuration
- log redaction policy

### Health

- Custom health: `GET /api/health` returns `OK`.
- Actuator health is available due to Actuator dependency/config.

---

## Resilience, Cloud, Cache, and Async

### Spring Cloud

- BOM: `spring-cloud-dependencies:2025.1.2`
- Dependencies:
  - `spring-cloud-starter`
  - `spring-cloud-starter-loadbalancer`
  - `spring-cloud-starter-circuitbreaker-resilience4j`

### Resilience4j

- Direct dependencies:
  - `resilience4j-bulkhead:2.1.0`
  - `resilience4j-spring-boot3:2.1.0`
- Usage found:
  - `@CircuitBreaker(name = "eccnService", fallbackMethod = "getEccnHistoryFallback")` in `EccnService`.

### Load Balancer

- Dependency and tests are present.
- No broad production service-client usage was identified in main code.

### Cache

- `spring-boot-starter-cache` and `caffeine:3.1.0` are present.
- App enables caching with `@EnableCaching`.
- Runtime config sets `spring.cache.type=none`, disabling caching.

### Async

- App enables async with `@EnableAsync`.
- Executor config:
  - core size: 5
  - max size: 10
  - queue capacity: 100
  - thread prefix: `Async-Executor-`
- No `@Async` usage was found in main Java source.

---

## Testing Stack

### Dependencies

- `spring-boot-starter-test`
- JUnit Jupiter
- Mockito, pinned via property `mockito.version=5.8.0`
- Byte Buddy agent, pinned as `byte-buddy.version=1.14.11`
- Testcontainers:
  - `org.testcontainers:mongodb:1.19.3`
  - `org.testcontainers:junit-jupiter:1.19.3`
  - `org.testcontainers:testcontainers:1.19.3`
- Reactor Test

### Test Styles Found

- Unit tests with Mockito and `@ExtendWith(MockitoExtension.class)`.
- Spring Boot context tests with `@SpringBootTest` and `@ActiveProfiles("test")`.
- Testcontainers MongoDB config.
- Resilience/circuit breaker tests.
- Load balancer tests.

### Test Configuration

`src/test/resources/application-test.properties` configures:

- local test MongoDB URI: `mongodb://localhost:27017/testdb`
- embedded Mongo version: `4.0.21`
- Resilience4j circuit breaker enabled
- Spring Cloud LoadBalancer enabled
- discovery disabled
- JDBC auto-config excluded

### Testing Documentation Mismatch

README says MockMvc is part of the test stack, but no MockMvc usage was found in `src/test`.

---

## Container and Local Development Stack

### Dockerfile

- Base image: `eclipse-temurin:21-jdk-jammy`
- Working directory: `/app`
- Copies Maven wrapper, `.mvn`, `pom.xml`, and `src`
- Builds with `./mvnw clean package -DskipTests`
- Exposes port `8080`
- Runs `java -jar target/eccn-management-service-0.0.1-SNAPSHOT.jar`

### Docker Compose

Services:

| Service | Image / Build | Ports | Purpose |
|---|---|---|---|
| `mongodb` | `mongo:7.0` | `27017:27017` | Local MongoDB |
| `eccn-management-service` | local build, `eccn-management-service:latest` | `8080:8080` | Spring Boot API |

Compose configuration:

- MongoDB root username/password: `root` / `secret`.
- MongoDB data volume: `mongodb_data:/data/db`.
- App env var: `SPRING_DATA_MONGODB_URI=mongodb://root:secret@mongodb:27017/eccn_management?authSource=admin`.
- App depends on MongoDB healthcheck.

Risk: Compose Mongo 7 healthcheck uses `mongo --eval`; MongoDB 7 images commonly use `mongosh`, so verify the healthcheck command.

---

## API Testing and Developer Tooling

### Postman / Newman

Assets:

- `postman/ECCN-Management.postman_collection.json`
- `postman/import-collection.sh`

Collection areas:

- Authentication
- Product Portfolio
- Classification
- Risk Assessment
- Compliance Operations

Default variables:

- `auth_url=http://localhost:8081/auth`
- `product_service_url=http://localhost:8080`
- `classification_service_url=http://localhost:8081`
- `risk_service_url=http://localhost:8082`
- `compliance_service_url=http://localhost:8083`

Risks:

- Collection references Keycloak-like token endpoint, but no auth service is provided in Compose.
- Collection references separate classification/risk/compliance service APIs that are not implemented by the current single service.
- `import-collection.sh` actually runs Newman tests and writes `postman/report.json`; it does not import a collection.

### Complexity Tooling

- `scripts/analyze_complexity.py` is a Python 3 cyclomatic complexity analyzer for Java.
- Supports text/json/csv-style output and threshold-based failure behavior.
- Reference guide exists in `references/complexity-guide.md`.

### IDE / Editor Assets

- VS Code settings and scripts exist under `.vscode/`.
- Eclipse project metadata exists: `.project`, `.classpath`, `.factorypath`, `.settings/`.
- Prompt/planning artifacts exist under `prompts/`.

### CI/CD

- README claims GitHub Actions.
- No `.github/workflows/` files were found in this checkout.

---

## Dependency Inventory

### Spring Boot Starters

- `spring-boot-starter-actuator`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-data-rest`
- `spring-boot-starter-security`
- `spring-boot-starter-web`
- `spring-boot-starter-cache`
- `spring-boot-starter-validation`
- `spring-boot-starter-logging`
- `spring-boot-starter-webflux`
- `spring-boot-starter-test` (test)

### Spring Cloud

- `spring-cloud-starter`
- `spring-cloud-starter-loadbalancer`
- `spring-cloud-starter-circuitbreaker-resilience4j`
- BOM: `spring-cloud-dependencies:2025.1.2`

### Third-Party Libraries

- `org.projectlombok:lombok:1.18.30`
- `com.github.ben-manes.caffeine:caffeine:3.1.0`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0`
- `io.github.java-diff-utils:java-diff-utils:4.12`
- `io.github.resilience4j:resilience4j-bulkhead:2.1.0`
- `io.github.resilience4j:resilience4j-spring-boot3:2.1.0`
- `io.projectreactor:reactor-test` (test)
- `org.testcontainers:mongodb:1.19.3` (test)
- `org.testcontainers:junit-jupiter:1.19.3` (test)
- `org.testcontainers:testcontainers:1.19.3` (test)

---

## Current Stack Mismatches and Risks

| Area | Finding | Impact |
|---|---|---|
| Java version | README says Java 17+, POM/Docker use Java 21 | Developer setup confusion |
| Maven version | README says Maven 3.8+, wrapper uses Maven 3.9.9 | Minor docs drift |
| Mongo version | README says MongoDB 6.0+, Compose uses 7.0, tests use 6.0 | Test/local parity gap |
| CI/CD | README claims GitHub Actions, no workflow found | Delivery automation absent or omitted |
| Security | Spring Security present, no explicit policy found | Default behavior and broad access uncertainty |
| Credentials | Local credentials committed | Security risk |
| Actuator | Exposes all endpoints and health details always | Production information disclosure risk |
| Postman | References auth and services not in Compose/current controllers | API test drift |
| CORS | No PATCH allowed, but product API uses PATCH | Browser clients may fail preflight |
| Cache | Cache dependencies/enabling present, runtime disabled | Unused/unclear performance strategy |
| WebFlux | Dependency present, no main-code usage found | Potential dependency bloat |
| Async | Configured, no `@Async` usage found | Unused/unclear concurrency strategy |
| Hystrix references | Config references Hystrix stream/settings while using Resilience4j | Legacy/stale configuration |
| Docker | Full JDK image, tests skipped, no non-root user | Image hardening and validation gaps |

---

## Recommended Tech Stack Actions

### Stabilize Build and Docs

1. Fix current compilation issues before expanding the stack.
2. Align README prerequisites with Java 21 and Maven wrapper usage.
3. Update Postman collection to match implemented endpoints or mark target APIs clearly.
4. Add or remove README CI/CD claims depending on whether GitHub Actions are intended.

### Harden Runtime

1. Add explicit Spring Security configuration with endpoint-level authorization.
2. Move MongoDB and admin credentials to environment/secret management.
3. Restrict Actuator endpoints by profile and authentication.
4. Add production logging/metrics/tracing strategy.

### Rationalize Dependencies

1. Remove WebFlux if no reactive client/server code is planned.
2. Remove or update Hystrix references.
3. Decide whether Caffeine caching should be enabled and where `@Cacheable` belongs.
4. Decide whether async execution is needed and add explicit `@Async` use cases or remove config.

### Improve Deployment

1. Add container health checks using `/actuator/health` or `/api/health` in Compose.
2. Add non-root container user and multi-stage/smaller runtime image.

---

## Evidence Index

Primary files inspected:

- `pom.xml`
- `.mvn/wrapper/maven-wrapper.properties`
- `README.md`
- `src/main/java/com/aciworldwide/eccn_management_service/EccnManagementServiceApplication.java`
- `src/main/java/com/aciworldwide/eccn_management_service/config/*.java`
- `src/main/java/com/aciworldwide/eccn_management_service/controller/*.java`
- `src/main/java/com/aciworldwide/eccn_management_service/model/*.java`
- `src/main/java/com/aciworldwide/eccn_management_service/repository/*.java`
- `src/main/java/com/aciworldwide/eccn_management_service/service/*.java`
- `src/main/resources/application.properties`
- `src/test/java/com/aciworldwide/eccn_management_service/**`
- `src/test/resources/application-test.properties`
- `Dockerfile`
- `compose.yaml`
- `postman/ECCN-Management.postman_collection.json`
- `postman/import-collection.sh`
- `scripts/analyze_complexity.py`
- `.vscode/*`

Subagent artifacts used:

- `.pi-subagents/artifacts/outputs/ff80e230-f5d9-4d2a-8a4f-078c93cf6135/tech-stack/build-language.md`
- `.pi-subagents/artifacts/outputs/ff80e230-f5d9-4d2a-8a4f-078c93cf6135/tech-stack/runtime-app-data.md`
- `.pi-subagents/artifacts/outputs/ff80e230-f5d9-4d2a-8a4f-078c93cf6135/tech-stack/deployment-ops.md`
