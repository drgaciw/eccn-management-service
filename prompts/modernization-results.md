# ECCN Management Service — Modernization Results & Backlog

## Metadata
- **Branch**: `feature/lane-a-modernization` (in-place, WIP preserved)
- **Date**: 2026-07-09
- **Plan**: `prompts/modernization-review-plan.md`
- **Validation tool**: GitNexus (`detect_changes`, `impact`) + Maven (`./mvnw compile`/`test`)

---

## 1. Implemented (Phase 0 + Lane A) — VERIFIED

### Build fix (Phase 0 prerequisite) — root cause
The build was broken (290 compile errors). Root cause: **Lombok annotation processing was not running** under the **JDK 25** toolchain while the project pinned **Lombok 1.18.30** (supports only up to JDK 21/23). The earlier "Eccn compiles" was an illusion from javac's default `-Xmaxerrs=100` truncation.

- Lombok `1.18.30` → `1.18.46` (JDK 25 support).
- Added explicit `maven-compiler-plugin` `<annotationProcessorPaths>` (lombok + spring-boot-configuration-processor) — modern best practice.
- Fixed 3 WIP gaps blocking compile: `EccnValidationException` (added backward-compatible 1-arg ctor), `GlossaryController` (added missing `GlossaryException` import), `EccnService.getEccnHistory` (placeholder — `findEccnHistory` had no data model).

### Lane A version upgrades (Context7-verified, compatibility-checked)
| Component | From | To |
|---|---|---|
| Spring Boot | 3.4.1 | **3.5.9** |
| Spring Cloud BOM | 2024.0.0 | **2025.0.0** (matches Boot 3.5) |
| Springdoc OpenAPI | 2.3.0 | **2.8.6** |
| Lombok | 1.18.30 | **1.18.46** |
| Testcontainers | 1.19.3 | **1.20.4** |
| Caffeine | 3.1.0 | **3.2.0** |
| Java Diff Utils | 4.12 | **4.15** |
| Resilience4j | 2.1.0 (direct pin) | **BOM-managed** (pin removed) |
| Mockito / Byte Buddy | pinned | **BOM-managed** (pins removed) |

### Config / dependency hygiene
- **Removed unused `spring-boot-starter-webflux`** (no reactive code; MVC + Tomcat is the real server).
- **CORS**: added `PATCH` (Product API uses PATCH).
- **Actuator hardened**: `include=*,hystrix.stream` → `health,info`; `show-details=always` → `when-authorized`.
- **Hystrix dead config** removed.
- **Credentials externalized** via env placeholders (local defaults kept): `MONGODB_URI`, `MONGODB_USERNAME`, `MONGODB_PASSWORD`, `SECURITY_ADMIN_NAME`, `SECURITY_ADMIN_PASSWORD`. This also **binds the Rancher `MONGODB_URI`** env var.
- **OpenAPI**: removed redundant `/api` server entry (was doubling the prefix to `/api/api/...` in Swagger).

### Verification evidence
- `./mvnw -DskipTests compile` → **BUILD SUCCESS**
- `./mvnw test` → **Tests run: 63, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS** (Spring Boot v3.5.9, Java 25, Mongo driver 5.5.2)
- GitNexus `detect_changes`: changes wired into expected flows (e.g. `CreateEccn→EccnException`, `WebConfig/addCorsMappings`).
- GitNexus `impact`: `getEccnHistory` LOW (0 impacted); `addCorsMappings` LOW (0 impacted); `EccnValidationException` HIGH (additive-only change — documented).

---

## 2. Review Backlog (Phase 1 — parallel subagents, read-only)

### Security (HIGH — defer to dedicated security work)
- **No `SecurityFilterChain` / `@EnableWebSecurity` / `@PreAuthorize` / `UserDetailsService` / `PasswordEncoder`** anywhere — app falls back to insecure HTTP Basic + single shared `admin/USER` credential. → Add explicit filter chain + stateless auth.
- **No OAuth2 resource server / JWT** despite Postman Keycloak bearer flow (`postman/...:9-47`). → Add `spring-boot-starter-oauth2-resource-server`.
- **No endpoint authorization matrix** across ~20 endpoints; `USER` can run compliance-impacting writes (`markVersionAsClassified`, bulk glossary import). → Per-endpoint/role authz.
- **CSRF enabled with Basic auth** will 403 non-GET writes once auth model is stateless.

### Data integrity (HIGH)
- **No indexes / unique constraints** on natural keys: `Eccn.commodityCode`, `GlossaryEntry.term`, `Product.name`, `ExportControl.moduleName`, `RiskAssessment.moduleName`. → `@Indexed(unique=true)`.
- **20 `@Transactional`** on a **standalone** Mongo (`localhost:27017`, no `MongoTransactionManager` bean) — silently no-op / will throw if a real TX is required. → Replica set + `MongoTransactionManager`, or remove annotations.
- **TOCTOU races**: `GlossaryService.checkDuplicateTerm`→save (lines 34-36, 175-181); `EccnService.createEccn` has **no** duplicate check at all. → Unique index + `DuplicateKeyException` handling.
- **`ModuleAnalysis`** persisted via `ClassificationHistoryRepository` without `@Document`/`@Id` (`AutomatedClassificationToolService:27-55`); `getLatestAnalysis` has no sort → order not guaranteed.

### API contract & deployment drift (HIGH/MED)
- **Postman drift**: references Keycloak token endpoint + `classification_service_url`/`risk_service_url`/`compliance_service_url` for services not implemented.
- **Rancher production-readiness gaps** (all 4 manifests): no probes, HPA, PDB, NetworkPolicy, `securityContext.runAsNonRoot`, Secret manifest (referenced but undefined), ServiceAccount/RBAC, Ingress.
- **Dockerfile**: full `21-jdk` image, `package -DskipTests`, runs as root. → `21-jre`, test in CI, non-root `USER`.
- **README drift**: claims MockMvc / GitHub Actions / Java 17 vs reality (no MockMvc, no `.github/workflows`, Java 21).
- **`import-collection.sh`**: misleadingly named — actually runs Newman tests.

---

## 2b. Backlog items addressed (next-steps orchestration — parallel subagents)

Three parallel subagents worked disjoint file surfaces; integrated + verified centrally.

### Data integrity (DONE)
- `@Indexed(unique=true)` added on natural keys: `Eccn.commodityCode`, `GlossaryEntry.term`, `Product.name`, `ExportControl.moduleName`, `RiskAssessment.moduleName`. GitNexus impact: LOW.
- `spring.data.mongodb.auto-index-creation=true` added — **NOTE: verified not to take effect in this setup** (collections retain only `_id_`). The `EccnDuplicateKeyTest` therefore ensures the unique index explicitly via `MongoTemplate.indexOps().ensureIndex(...)`. **Production still needs the indexes created via a migration tool (e.g. Mongock) or ops** — the `@Indexed` annotations + duplicate-handling code are in place, but runtime enforcement is not guaranteed until indexes are provisioned.
- `ModuleAnalysis` inner class: added `@Document` + `@Id String id` (now a valid `MongoRepository` entity). `@Transactional` left as-is (replica-set decision pending).
- Open: TOCTOU duplicate handling (`DuplicateKeyException` catch in `GlossaryService`/`EccnService`) + replica-set/`MongoTransactionManager` decision.

### Deployment hardening (DONE)
- `Dockerfile` → multi-stage (JDK build → JRE runtime) + non-root `USER app`.
- All 4 Rancher manifests: liveness/readiness probes (`/actuator/health`), container `securityContext` (runAsNonRoot, readOnlyRootFilesystem, allowPrivilegeEscalation:false, runAsUser:1000) + `/tmp` emptyDir.
- `README.md`: Java 17→21, MockMvc→Mockito/Testcontainers, removed GitHub-Actions claim.
- `postman/import-collection.sh`: comments corrected (runs tests, not import).

### Security baseline (DONE — JWT/OIDC still gated)
- New `config/SecurityConfig.java`: explicit `SecurityFilterChain` — stateless, CSRF disabled, HTTP Basic (existing in-memory user), permit `/actuator/health|info`, `/swagger-ui/**`, `/v3/api-docs/**`, `/api/health`; all else `authenticated()`. Replaces insecure auto-config defaults. GitNexus impact: LOW (new bean, 0 impacted).
- Open (gated, needs auth provider): JWT/OIDC resource server (Keycloak), per-role `@PreAuthorize` matrix, `PasswordEncoder`.

### Verification
- `./mvnw compile` → BUILD SUCCESS; `./mvnw test` → **63 run, 0 failures**.
- GitNexus re-index (1,420 nodes) + `detect_changes`: 30 files, 205 symbols, 38 flows; `securityFilterChain` impact LOW.

---

## 2c. Further hardening (next-steps orchestration — parallel subagents)

- **Data completion**: `DuplicateKeyException` handling added at save sites — `GlossaryService.createEntry`/`importBulkEntries` → `GlossaryException(DUPLICATE_TERM)` (pre-check retained); `EccnService.createEccn` → `EccnValidationException(DUPLICATE_CODE)`. GitNexus impact: LOW on all three methods. Completes the unique-index work (no more TOCTOU gap).
- **CI**: added `.github/workflows/ci.yml` (push/PR → `./mvnw -B -ntp clean verify` on JDK 21, Maven cache) — fills the README's unmet "GitHub Actions" claim.
- **DevOps**: created `.dockerignore` (keeps build context lean; excludes `.gitnexus/`, `target/`, IDE/docs); fixed `compose.yaml` Mongo 7 healthcheck `mongo`→`mongosh`.
- **Verification**: `./mvnw test` → **63 run, 0 failures**; GitNexus `detect_changes` 32 files / 208 symbols / 54 flows.

---

## 2d. Decisions executed + observability (next-steps orchestration)

- **`@Transactional` cleanup (DECISIONS.md #1 executed)**: removed all 20 no-op `@Transactional` annotations across GlossaryService/DocumentRecordService/ProductService/ExportControlService/RiskAssessmentService (+ unused imports). GitNexus impact LOW on all 20 methods. They were silent no-ops (no MongoTransactionManager on standalone Mongo); re-add as one coordinated change with a replica set + `MongoTransactionManager` if transactions are needed.
- **API docs**: `OpenApiConfig` now declares an HTTP Basic `SecurityScheme` + global `SecurityRequirement` so Swagger UI reflects auth. Impact LOW.
- **README**: modernized — Java 21 + `./mvnw`, Configuration env-var table, Build & Test (`./mvnw clean verify`, CI), Security note (HTTP Basic; JWT/OIDC planned).
- **Observability**: added `micrometer-registry-prometheus`; Actuator now exposes `health,info,prometheus` (`/actuator/prometheus` is authenticated). Structured logging left as a profile decision (not forced on local console).
- **Verification**: `./mvnw test` → **67 run, 0 failures**; GitNexus `detect_changes` 36 files / 213 symbols / 60 flows; sanity: 0 `@Transactional` in main, prometheus dep present.

---

## 3. Phase 3 — Lane B (Spring Boot 4.x) — GATED / DEFERRED

**Not started.** Spring Boot `4.1.0` is the latest GA but is a **major migration** (Spring Framework 7, Jakarta EE 11, removed/renamed auto-configurations). It requires:
1. Impact-analyze every controller/service/repo for 4.x breakage (GitNexus `impact`).
2. Migrate Springdoc 2.x → 3.x.
3. Re-validate Lombok against the chosen JDK.
4. Re-run full 63-test suite green as the gate.

**Gate**: Lane B proceeds only after the §2 backlog (security + data-integrity + deployment) is addressed and Lane A is merged.
