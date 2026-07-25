# Plan: Spring Boot 3.5.9 → 4.0.x Upgrade

## Context

Upgrade the ECCN Management Service from Spring Boot 3.5.9 to the latest
Spring Boot 4.0.x. The project is a single-module Spring MVC + MongoDB +
Spring Security + Springdoc OpenAPI service with 46 main source files and
17 test files (71 passing tests).

### Current Stack
| Component | Current Version | Target Version |
|-----------|----------------|----------------|
| Spring Boot | 3.5.9 | 4.0.x (latest) |
| Spring Cloud | 2025.0.0 | 2025.1.2 |
| springdoc-openapi | 2.8.6 | 3.0.3 |
| Spring Framework | 6.2.x | 7.0.x |
| Spring Security | 6.5.x | 7.0.x |
| Spring Data MongoDB | 4.5.x | 5.x |
| Java | 21 | 21 (unchanged) |
| Jackson | 2.19.x (auto) | 3.x (auto via Boot 4) |

### Decision: Direct Modular Starters
User chose direct migration to new modular starters (no `spring-boot-starter-classic` intermediate).

## Key Breaking Changes (from research)

1. **Starter renames**: `spring-boot-starter-web` → `spring-boot-starter-webmvc`
2. **MongoDB property renames**: `spring.data.mongodb.uri` → `spring.mongodb.uri`, etc.
3. **Jackson 3**: default JSON library, package `com.fasterxml.jackson` → `tools.jackson`
4. **Spring Cloud**: 2025.0.x incompatible with Boot 4; requires 2025.1.2+
5. **springdoc**: 2.x incompatible with Boot 4; requires 3.x
6. **Actuator**: liveness/readiness probes enabled by default
7. **`@MockBean`/`@SpyBean` removed**: project does NOT use these (confirmed)
8. **`@SpringBootTest` no longer provides MockMVC**: project does NOT use MockMVC (confirmed)

### What This Project Does NOT Need
- No `@MockBean`/`@SpyBean` migration (uses `@Mock`/`@InjectMocks`)
- No MockMVC migration (not used)
- No direct Jackson imports in source (auto-config only)
- No `TestRestTemplate`/`WebClient` usage
- No Undertow (uses Tomcat)
- No Spring Session
- No JPA/Hibernate

---

## Tasks

### Phase 1: POM Dependency Updates

**Step 1.1 — Update parent and Spring Cloud version**
- `pom.xml`: Change `spring-boot-starter-parent` from `3.5.9` → `4.0.x` (latest patch)
- `pom.xml`: Change `spring-cloud.version` from `2025.0.0` → `2025.1.2`
- `pom.xml`: Update `spring-boot-configuration-processor` version reference from `3.5.9` → new Boot version

**Step 1.2 — Rename deprecated starters**
| Current Starter | New Starter |
|----------------|-------------|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| (others stay the same) | — |

Starters that keep the same name: `spring-boot-starter-actuator`, `spring-boot-starter-data-mongodb`, `spring-boot-starter-data-rest`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-cache`, `spring-boot-starter-test`, `spring-boot-devtools`, `spring-boot-configuration-processor`.

**Step 1.3 — Update springdoc-openapi**
- `pom.xml`: Change `springdoc-openapi-starter-webmvc-ui` from `2.8.6` → `3.0.3`

**Step 1.4 — Add properties migrator (temporary)**
- `pom.xml`: Add `spring-boot-properties-migrator` as runtime dependency
- This will log diagnostics at startup for any renamed properties
- **Remove after migration is verified**

**Step 1.5 — Compile check**
```bash
./mvnw clean compile
```
Fix any compilation errors. Likely candidates: none (no direct framework API breaks in this codebase), but verify.

### Phase 2: application.properties Migration

**Step 2.1 — Rename MongoDB properties**

| Current Property | New Property |
|-----------------|-------------|
| `spring.data.mongodb.uri` | `spring.mongodb.uri` |
| `spring.data.mongodb.authentication-database` | `spring.mongodb.authentication-database` |
| `spring.data.mongodb.username` | `spring.mongodb.username` |
| `spring.data.mongodb.password` | `spring.mongodb.password` |
| `spring.data.mongodb.auto-index-creation` | (unchanged — stays `spring.data.mongodb.auto-index-creation`) |

File: `src/main/resources/application.properties`

**Step 2.2 — Review actuator property changes**
- Liveness/readiness probes are now enabled by default in Boot 4
- If probes are unwanted, add: `management.endpoint.health.probes.enabled=false`
- Current actuator config should work as-is

**Step 2.3 — Verify with properties-migrator**
```bash
./mvnw spring-boot:run
```
Check startup logs for property migration warnings. Fix any remaining renamed properties.

### Phase 3: Code Changes

**Step 3.1 — Verify SecurityConfig compatibility**
- File: `src/main/java/.../config/SecurityConfig.java`
- Spring Security 7.0 (Boot 4) changes: the lambda DSL used (`csrf(csrf -> csrf.disable())`) is still supported
- Verify `SessionCreationPolicy`, `authorizeHttpRequests` still compile
- Run GitNexus impact on `securityFilterChain` before editing:
  ```bash
  node .gitnexus/run.cjs impact --repo eccn-management-service -d upstream securityFilterChain
  ```

**Step 3.2 — Verify OpenApiConfig compatibility**
- File: `src/main/java/.../config/OpenApiConfig.java`
- springdoc 3.x uses the same `io.swagger.v3.oas.models.*` API
- Should compile without changes

**Step 3.3 — Verify WebConfig compatibility**
- File: `src/main/java/.../config/WebConfig.java`
- `WebMvcConfigurer` still exists in Spring Framework 7
- Should compile without changes

**Step 3.4 — Verify controller/service/repository layer**
- No direct framework API breaks expected (standard `@RestController`, `@RequestMapping`, etc.)
- Run full compile to verify

### Phase 4: Testing & Validation

**Step 4.1 — Run full test suite**
```bash
./mvnw clean test
```
All 71 tests should pass. Key areas to watch:
- `@SpringBootTest` context loading (MongoDB connection, bean wiring)
- `@ExtendWith(MockitoExtension.class)` tests (Mockito 5+ should work)
- `GlossaryDuplicateKeyTest` / `EccnDuplicateKeyTest` (MongoDB integration)

**Step 4.2 — Verify Jackson serialization**
- No direct Jackson imports in source, but verify:
  - MongoDB document serialization round-trips correctly
  - REST API JSON responses are correct
- If Jackson 3 causes issues, add stop-gap: `spring-boot-jackson2` module

**Step 4.3 — Verify Actuator endpoints**
- Confirm `/actuator/health`, `/actuator/info`, `/actuator/prometheus` still work
- Note: liveness/readiness probes now appear under health groups by default

**Step 4.4 — Verify OpenAPI/Swagger UI**
- Confirm `/swagger-ui.html` and `/v3/api-docs` render correctly
- springdoc 3.x may have different property names — check if `springdoc.*` properties need updates

**Step 4.5 — Remove properties-migrator**
- Once all properties are migrated and verified, remove `spring-boot-properties-migrator` dependency from `pom.xml`

### Phase 5: Docker & Deployment Updates

**Step 5.1 — Update Dockerfile**
- File: `Dockerfile`
- Java 21 is still supported by Boot 4 — no image change needed
- Verify: `eclipse-temurin:21-jdk-jammy` and `eclipse-temurin:21-jre-jammy` still work

**Step 5.2 — Update compose.yaml**
- File: `compose.yaml`
- No expected changes (MongoDB connection via env vars)

**Step 5.3 — Update Rancher manifests**
- Files: `rancher/*.yaml`
- Verify image tags and env vars still correct

**Step 5.4 — Update CI workflow**
- File: `.github/workflows/ci.yml`
- Verify Java 21 setup-action version is compatible

### Phase 6: Documentation

**Step 6.1 — Update tech-stack.md and README**
- Update Spring Boot version references
- Update Spring Cloud version
- Update springdoc version

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| springdoc 3.x property name changes | Medium | Low | Check springdoc 3.x docs; properties-migrator will warn |
| MongoDB driver version bump | Low | Medium | Run integration tests (EccnDuplicateKeyTest, GlossaryDuplicateKeyTest) |
| Spring Security 7.0 API changes | Low | Medium | SecurityConfig uses standard DSL; compile will catch |
| Jackson 3 serialization differences | Low | Medium | Run full test suite; add jackson2 stop-gap if needed |
| Spring Cloud 2025.1.x compatibility | Low | High | Spring Cloud 2025.1.2 is explicitly certified for Boot 4.0.x/4.1.x |

## Validation Checklist

- [ ] `./mvnw clean compile` succeeds with zero warnings
- [ ] `./mvnw clean test` — all 71 tests pass
- [ ] `./mvnw spring-boot:run` — application starts without errors
- [ ] Properties-migrator logs show zero remaining warnings
- [ ] Properties-migrator dependency removed
- [ ] `/actuator/health` returns 200
- [ ] `/swagger-ui.html` renders
- [ ] GitNexus `detect_changes` shows expected scope only

## Open Questions (resolve during execution)

1. **springdoc 3.x properties**: Verify if `springdoc.swagger-ui.path` and `springdoc.api-docs.path` property names changed in springdoc 3.x.
2. **MongoDB UUID/BigDecimal representation**: Boot 4 removes Spring Data MongoDB defaults for UUID/BigDecimal representation. Verify if any model fields use these types (currently `String` IDs, so likely no impact).
3. **`spring.main.allow-bean-definition-overriding`**: Verify this property still exists in Boot 4 or has been renamed.
