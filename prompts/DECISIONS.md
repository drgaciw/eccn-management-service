# ECCN Management Service — Decision Spec (Gated Modernization)

Three remaining gated items from `modernization-results.md`. Each decision below is documented-only; no code changes are made by this spec.

---

## Decision 1 — MongoDB `@Transactional` topology

**Background**: 20 `@Transactional` annotations span the services — `GlossaryService` (4), `DocumentRecordService` (5), `ProductService` (3), `ExportControlService` (4), `RiskAssessmentService` (4) — on a STANDALONE Mongo (`mongodb://localhost:27017`, `application.properties:8`) with NO `MongoTransactionManager` bean defined anywhere in `src/main/java`. With no manager, Spring Data Mongo silently ignores them (no-op). The test suite uses standalone Testcontainers Mongo (`pom.xml:56-60`).

**Options**:
- **(a) Keep no-op annotations + document replica-set requirement** — annotations stay misleading; readers believe multi-doc atomicity exists when it does not.
- **(b) Remove annotations** — Mongo single-document writes are atomic by default; multi-doc writes are not, but nothing here currently requires cross-doc rollback. Eliminates the false-safety signal. Clean, zero-runtime-risk.
- **(c) Replica set in dev/prod + `MongoTransactionManager` bean** — gives real ACID multi-doc transactions, BUT standalone servers reject sessions/transactions, so this would make the annotations THROW and break the standalone Testcontainers test suite until both dev/prod AND tests run replica sets.

**Recommended**: **(b) Remove annotations now.** Re-add them together with a `MongoTransactionManager` bean + replica-set topology (dev, prod, and Testcontainers `withReplicaSet`) as a single coordinated change when a replica set is adopted.

**Unblocks**: Eliminates a latent correctness hazard (silent no-ops) and the breakage trap in (c). Clears the §2 data-integrity open item (`modernization-results.md:80`).

---

## Decision 2 — JWT/OIDC resource server (Keycloak)

**Background**: The Postman collection expects a Keycloak password-grant flow — `POST {{auth_url}}/protocol/openid-connect/token` with `grant_type=password`, `client_id=eccn-management-client` (`postman/ECCN-Management.postman_collection.json:43,38`) and `Bearer {{access_token}}` on requests (`:65`). No resource server exists: `pom.xml` has no `spring-boot-starter-oauth2-resource-server`, and `SecurityConfig.java:29` uses HTTP Basic. A JWT issuer URI / Keycloak realm URL is **REQUIRED and cannot be invented** — `{{auth_url}}` is an unresolved collection variable (`:191`).

**Options / steps**:
- **Defer (gate on a real issuer URL)** — no security regression vs. current state (HTTP Basic baseline already in place); avoids a fabricated/failing JWK endpoint.
- **Implement once issuer provided** — exact steps:
  - Add `spring-boot-starter-oauth2-resource-server` dependency.
  - In `SecurityConfig`, add `.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))` (replace/combine with `.httpBasic`).
  - Config keys in `application.properties`:
    - `spring.security.oauth2.resourceserver.jwt.issuer-uri=<https://…/realms/<realm>>`
    - (optional) `spring.security.oauth2.resourceserver.jwt.jwk-set-uri=<https://…/protocol/openid-connect/certs>`
  - Add per-role `@PreAuthorize` matrix on compliance-impacting writes (`@EnableMethodSecurity` already present, `SecurityConfig.java:16`).

**Recommended**: **Defer until a Keycloak issuer URL is supplied.** Then implement the steps above.

**Unblocks**: The §2 security open item (`modernization-results.md:90`) — real bearer-token auth, role-based authorization matrix, and alignment of the service with its Postman contract.

---

## Decision 3 — Spring Boot 3.5.x → 4.x (Lane B)

**Background**: App is on Spring Boot **3.5.9** (`pom.xml:8`), Spring Framework 6, Java 21 (`pom.xml:17`), Springdoc **2.8.6** (`pom.xml:121`). Spring Boot **4.1.0** is latest GA but is a major migration: Spring Framework 7, Jakarta EE 11, removed/renamed auto-configurations, Springdoc 2.x → 3.x, and Lombok/JDK re-validation.

**Options**:
- **(a) Stay on 3.5.x LTS** — stable, the 63-test suite is green (`modernization-results.md:43`); no migration risk; receives LTS fixes.
- **(b) Plan phased 4.x migration, gated on green 3.5.x** — unlocks Spring Framework 7 / Jakarta EE 11, but requires: per-symbol GitNexus `impact` analysis on every controller/service/repo for 4.x breakage; Springdoc 2.x→3.x; Lombok-vs-JDK re-validation; full 63-test suite re-green as the gate (`modernization-results.md:110-114`).

**Recommended**: **(a) Stay on 3.5.x now.** Pursue (b) as a separate, dedicated effort — only after Lane A is merged and the §2 backlog is resolved — driven by per-symbol GitNexus impact analysis (HIGH-risk symbols flagged before edits per project impact rule).

**Unblocks**: Closes out Lane B as an explicitly-scoped future effort rather than an open-ended question; keeps the current release on a supported, tested LTS line.
