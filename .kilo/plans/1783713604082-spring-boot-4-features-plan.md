# Plan: Adopt High-Impact Spring Boot 4.x Features

## Context

ECCN Management Service was upgraded from Spring Boot 3.5.9 → 4.0.7 (pom.xml, MongoDB properties, springdoc 3.0.3). The upgrade was minimal — just dependency and property renames. The codebase now needs selective refactoring to adopt high-impact Spring Boot 4.x features.

GitNexus confirms index is up-to-date (d952df4), 71 tests pass, no test uses HTTP clients or `@MockBean`.

### Current State
| Area | Current | Target |
|------|---------|--------|
| Threading | Platform threads (Tomcat default) | Virtual threads |
| API versioning | Path-based (`/api/v1/eccn`) | `spring.mvc.apiversion.*` multi-strategy |
| Testcontainers | Manual `@DynamicPropertySource` + `@PostConstruct`/`@PreDestroy` | `@ServiceConnection` |
| Observability | Micrometer Prometheus (metrics only) | + OpenTelemetry tracing (OTLP) |
| Test HTTP | None (service-level autowired tests) | `RestTestClient` where applicable |
| Logging | Console always on | `logging.console.enabled` explicit |

---

## Tasks

### 1. Virtual Threads — Zero-Code Config Change

**Rationale**: Spring Boot 4 + Tomcat 11 auto-configures virtual threads when enabled. All I/O-bound controller requests benefit without code changes.

- **File**: `src/main/resources/application.properties`
  - Add: `spring.threads.virtual.enabled=true`
- **Verify**: `./mvnw spring-boot:run` — check startup log for "Virtual threads" in Tomcat thread pool
- **Risk**: None. Virtual threads are backward-compatible with platform threads. Existing async executor pool (`spring.task.execution.*`) still works.
- **Test**: `./mvnw test` — all 71 tests should pass. Virtual threads don't change semantics.

### 2. `@ServiceConnection` for Testcontainers

**Rationale**: `MongoDBTestConfig` (98 lines) manually manages container lifecycle with `@PostConstruct`/`@PreDestroy`/`@DynamicPropertySource`. Spring Boot 4's `@ServiceConnection` auto-registers the container's connection properties, cutting ~60 lines of boilerplate.

**Affected file**: `src/test/java/.../config/MongoDBTestConfig.java`

**GitNexus impact check** before editing:
```bash
node .gitnexus/run.cjs impact --target "MongoDBTestConfig" --repo eccn-management-service
```

**Refactoring**:
- Replace manual container lifecycle with `@ServiceConnection` on a `@Bean` returning `MongoDBContainer`
- Remove `@PostConstruct startContainer()`, `@PreDestroy stopContainer()`, `@DynamicPropertySource setProperties()`, `staticMongoDBContainer` field
- Keep `@TestConfiguration`, Docker socket detection, and `Slf4jLogConsumer`
- The property `spring.data.mongodb.uri` → `spring.mongodb.uri` on line 91 (already renamed in application-test.properties but missed in MongoDBTestConfig)

**Before/After**:

```java
// Before (~98 lines, manual lifecycle)
@DynamicPropertySource
static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", () -> staticMongoDBContainer.getReplicaSetUrl());
}

// After (~30 lines, @ServiceConnection)
@Bean
@ServiceConnection
MongoDBContainer mongoDBContainer() {
    return new MongoDBContainer(DockerImageName.parse("mongo:6.0"))
        .withLogConsumer(new Slf4jLogConsumer(logger))
        .withReuse(true);
}
```

**Tests using this config**: `EccnDuplicateKeyTest`, `GlossaryDuplicateKeyTest`, `EccnManagementServiceApplicationTests`, `LoadBalancerTest`

**Verification**: `./mvnw test -Dtest="EccnDuplicateKeyTest,GlossaryDuplicateKeyTest"` — MongoDB container starts and tests pass.

### 3. API Versioning

**Rationale**: Spring Boot 4 adds `spring.mvc.apiversion.*` auto-config for header/param/media-type versioning. Controllers currently use path-based versioning (`/api/v1/eccn`). Goal: add a second non-breaking version strategy while keeping existing paths working.

**Strategy**: Dual-path — keep existing `/api/v1/...` paths AND add new `/api/...` paths with `@ApiVersion`. Old paths get deprecation notices. No downtime.

**File**: `src/main/resources/application.properties`
```properties
spring.mvc.apiversion.type=HEADER
spring.mvc.apiversion.header=X-API-Version
spring.mvc.apiversion.default=1
```

**Step 1**: Add version-aware controller beans (new paths):
```java
@RestController
@RequestMapping("/api/eccn")
@ApiVersion("1")
public class EccnControllerV2 { /* delegates to EccnService */ }
```

**Step 2**: Mark old controllers as deprecated (keep working):
```java
@RestController
@RequestMapping("/api/v1/eccn")
@Deprecated
@Tag(name = "ECCN Management (deprecated)", description = "Migrate to /api/eccn with X-API-Version: 1 header")
public class EccnController { /* existing code unchanged */ }
```

**Affected controllers** (GitNexus impact each before editing):
| Controller | Old Path (keep) | New Path (add) | Version |
|------------|----------------|----------------|---------|
| `EccnController` | `/api/v1/eccn` | `/api/eccn` | `1` |
| `GlossaryController` | `/api/v1/glossary` | `/api/glossary` | `1` |
| `ProductController` | `/api/products` | (already versionless) | — |
| `CryptoClassificationController` | `/api/crypto-classification` | (already versionless) | — |

**Migration timeline** (document only — no auto-removal):
- Phase 1 (now): Add `@ApiVersion` controllers, deprecate old
- Phase 2 (future): Remove old controllers after clients migrate
- `HealthController` unchanged

**SecurityConfig update**: Adjust `requestMatchers()` if paths change.

### 4. OpenTelemetry Starter

**Rationale**: Adds distributed tracing on top of existing Prometheus metrics. Enables end-to-end request tracing across microservices.

**File**: `pom.xml`
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

**File**: `src/main/resources/application.properties`
```properties
management.opentelemetry.resource-attributes.service.name=eccn-management-service
management.tracing.sampling.probability=1.0
```

**File**: `compose.yaml` — Add optional OTLP collector for local dev:
```yaml
  otel-collector:
    image: otel/opentelemetry-collector-contrib:latest
    ports:
      - "4317:4317"
      - "4318:4318"
```

**Verification**: `./mvnw spring-boot:run` — check `/actuator` exposes tracing endpoints. No existing test breaks.

### 5. RestTestClient (Low Priority)

**Rationale**: Spring Framework 7's `RestTestClient` replaces `TestRestTemplate` for integration tests. However, this codebase doesn't use `TestRestTemplate` or HTTP-level tests — all tests inject services directly. Only `LoadBalancerTest` uses `RANDOM_PORT`.

**Recommendation**: Document as available but skip implementation until HTTP-level integration tests are added. No immediate code changes.

### 6. Additional Property Improvements

**File**: `src/main/resources/application.properties`
```properties
# New Boot 4 property — explicit console logging control
logging.console.enabled=true

# Liveness/readiness probes enabled by default in Boot 4
# Add explicit config for clarity:
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
```

**File**: `src/test/java/.../config/MongoDBTestConfig.java:91`
- Fix leftover `spring.data.mongodb.uri` → `spring.mongodb.uri` (was missed in Phase 2 of previous upgrade)

---

## Risk Assessment

| Feature | Risk | Mitigation |
|---------|------|------------|
| Virtual threads | Low | Config-only toggle; revertible |
| @ServiceConnection | Low | Equivalent behavior; test suite covers |
| API versioning (dual-path) | Low | Additive; old paths unchanged, new paths alongside |
| OpenTelemetry | Low | Opt-in starter; no code changes |
| RestTestClient | None | Skipped (not applicable) |

## Validation Checklist

- [ ] `spring.threads.virtual.enabled=true` added; startup confirms virtual threads
- [ ] GitNexus `impact` run on `MongoDBTestConfig` before editing
- [ ] `@ServiceConnection` refactoring passes `EccnDuplicateKeyTest` and `GlossaryDuplicateKeyTest`
- [ ] `spring.data.mongodb.uri` → `spring.mongodb.uri` fixed in MongoDBTestConfig:91
- [ ] API versioning: dual-path controllers added, old controllers `@Deprecated`, `X-API-Version` header works
- [ ] `spring-boot-starter-opentelemetry` added; OTLP endpoints accessible
- [ ] `logging.console.enabled` and actuator probe properties added
- [ ] `./mvnw clean test` — all 71 tests pass
- [ ] GitNexus `detect-changes` shows expected scope only

## Open Questions

1. **OTLP endpoint**: Where does the OTLP collector run? Production endpoint not defined in current rancher manifests. For local dev, optional collector in compose.yaml.
