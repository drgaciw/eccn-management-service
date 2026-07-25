# ECCN Management Service — Modernization Review Plan

## Plan Metadata

- **Repository**: `eccn-management-service`
- **Generated**: 2026-07-09
- **Author**: Code-modernization expert (superpowers + GitNexus + Context7)
- **Method**: Context7 latest-stable lookup, GitNexus index inspection, parallel-agent dispatch design
- **Inputs**: `tech-stack.md`, `pom.xml`, GitNexus index (stale), Context7 version data

> This is a **review + planning** deliverable. It does NOT modify code. All version claims below are Context7-verified as of the generation date.

---

## 1. Goal

1. Use **GitNexus** (knowledge graph + impact analysis) to review the codebase via **parallel subagents**, each owning an independent functional domain.
2. Establish the **latest stable version** of every stack element in `tech-stack.md` and verify **cross-version compatibility** before any upgrade is attempted.
3. Produce a safe, phased upgrade path (conservative in-place bumps first; major-version migration as a separate, gated phase).

---

## 2. Prerequisite: Refresh the GitNexus Index (blocking)

The index is **stale** (indexed commit `4d22748`, working tree at `c6e801c`) and the host has **multiple repos indexed**, so every CLI call needs `--repo eccn-management-service`.

```bash
node .gitnexus/run.cjs analyze                       # rebuild knowledge graph
node .gitnexus/run.cjs status --repo eccn-management-service   # confirm fresh
```

Do not start any subagent review until `status` reports **fresh**. Impact analysis on a stale graph produces wrong blast radius.

---

## 3. Latest-Stable Stack & Compatibility Matrix (Context7-verified)

The project currently runs a **Spring Boot 3.4.1** cluster that is internally consistent (Spring Cloud `2024.0.0` is the correct train for Boot 3.4.x). The table below gives the latest stable release and the **compatible target** for two upgrade lanes.

### Version mapping

| Layer | Current (`pom.xml`) | Latest Stable (Context7) | Conservative target (Lane A) | Aggressive target (Lane B) | Notes |
|---|---|---:|---:|---:|---|
| Java | 21 | 21 LTS (25 GA also available) | **21** | **25** | Keep 21 for Lane A; 21 is the baseline for all targets |
| Spring Boot | 3.4.1 | **4.1.0** (4.0.0 GA 2025-11-20) | **3.5.9** | **4.1.0** | 4.x is a major migration (Framework 7, Jakarta EE 11) |
| Spring Cloud BOM | 2024.0.0 | 2025.0.x | **2025.0.x** | 2026.x (when GA) | Train must match Boot: 2024.0↔3.4, 2025.0↔3.5 |
| Spring Cloud CircuitBreaker | managed | **5.0.x** (SC 2025.0) | managed | managed | Comes from BOM |
| Spring Security | managed | managed by Boot | managed | managed | Never pin; inherit from Boot |
| Spring Data MongoDB | managed | managed by Boot | managed | managed | Inherit from Boot |
| Spring Web / WebFlux | managed | managed by Boot | managed | managed | **Drop WebFlux** if no reactive code (confirmed unused) |
| Spring Boot Actuator | managed | managed by Boot | managed | managed | Harden exposure regardless of version |
| Springdoc OpenAPI | 2.3.0 | 2.8.x (Boot 3.5) | **2.8.x** | 3.x (Boot 4) | Must match Boot minor |
| Resilience4j | **2.1.0 (direct pin)** | **2.3.0** (SC BOM) | **2.3.0** | 2.3.0 | **Remove direct pin**, let SC BOM manage |
| Caffeine | 3.1.0 | 3.2.x | **3.2.x** | 3.2.x | Keep only if caching is re-enabled |
| Lombok | 1.18.30 | 1.18.38 | **1.18.38** | 1.18.38 | Must support annotation processor on chosen JDK |
| Testcontainers | 1.19.3 | 1.20.x | **1.20.x** | 1.20.x | Align test Mongo image across compose/tests |
| Java Diff Utils | 4.12 | 4.15 | **4.15** | 4.15 | |
| Mockito (property pin) | 5.8.0 (pinned) | managed by Boot | **remove pin** | remove pin | Inherit from Boot BOM |
| Byte Buddy (property pin) | 1.14.11 (pinned) | managed by Boot | **remove pin** | remove pin | Inherit from Boot BOM |
| Maven | 3.9.9 (wrapper) | 3.9.x | 3.9.9 | 3.9.9 | Boot 3.5 requires Maven ≥ 3.6.3 |
| MongoDB (runtime) | 7.0 compose / 6.0 test | 7.0 / 8.0 | **7.0 (unify)** | 8.0 | Fix test/local parity gap |
| Docker base | `eclipse-temurin:21-jdk-jammy` | JRE + multi-stage | `21-jre` | `25-jre` | Drop JDK runtime image; add non-root user |

### Critical compatibility rules (do not violate)

1. **Spring Cloud train ↔ Spring Boot minor is fixed.** `2024.0.x↔3.4.x`, `2025.0.x↔3.5.x`. Bumping Boot without bumping the train (or vice-versa) breaks resolution.
2. **Never override managed versions of Security / Data / Mockito / Byte Buddy** unless a CVE forces it — they are curated by the Boot BOM for a reason.
3. **Springdoc minor must track the Boot minor.** Springdoc 2.3.0 is for Boot 3.3/3.4; on Boot 3.5 use 2.8.x.
4. **Resilience4j direct pin (2.1.0) conflicts with the SC BOM.** Remove the explicit `<version>` so the BOM-managed value wins; otherwise the bulkhead/boot3 artifacts can desync from circuitbreaker.
5. **Spring Boot 4.x is not a patch upgrade.** It requires Spring Framework 7, Jakarta EE 11, and removes/renames several auto-configurations. It is Lane B, fully gated behind Lane A + impact analysis.
6. **Lombok must be validated against the chosen JDK** every time the JDK changes — it is the documented "compile-risk area."

---

## 4. GitNexus Parallel-Review Strategy

Per the dispatching-parallel-agents pattern: the codebase splits into **independent domains** with no shared edit surface, so each can be reviewed concurrently. GitNexus provides the blast-radius context each subagent needs instead of blind grepping.

### Review is read-only

All subagents run `impact`/`context`/`query`/`explain` (analysis only). No edits. Findings are returned as structured reports for the orchestrator to merge into a remediation backlog.

### 4.1 Parallel domain map

| # | Subagent | GitNexus entry points | Domain scope | Returns |
|---|---|---|---|---|
| 1 | **Build & dependency hygiene** | `impact: maven-compiler-plugin`, `pom.xml` symbols | POM pin removals, BOM alignment, WebFlux/Hystrix removal, Caffeine/Async decision | Dependency diff plan + risk |
| 2 | **ECCN classification domain** | `query: "ECCN classification"`, `impact: EccnService`, `context: EccnController` | ECCN CRUD, validateEccn, circuit-breaker fallback, format exceptions | Dead code, @CircuitBreaker correctness, exception flows |
| 3 | **Product portfolio domain** | `query: "product"`, `impact: ProductController`, `context: ProductRepository` | Product CRUD, **PATCH method**, CORS gap, repository methods | PATCH/CORS defect, repo-method drift |
| 4 | **Persistence & data integrity** | `query: "MongoDB transaction"`, `impact: *Repository`, `context: ModuleAnalysis` | Indexes, @Transactional on Mongo, ModuleAnalysis @Document/@Id gap, duplicate-check races | Data-integrity risks + migration needs |
| 5 | **Security & auth** | `query: "security filter chain"`, `impact: SecurityConfig` (if any), `explain` taint on controllers | Missing SecurityFilterChain, default admin/admin, JWT/OIDC gaps, endpoint matrix | Security remediation list |
| 6 | **Observability & ops** | `query: "actuator health"`, config symbols | Actuator exposure, Hystrix references, logging/tracing gaps, custom /api/health | Hardening checklist |
| 7 | **Deployment & config drift** | `rancher/*.yaml`, `compose.yaml`, `application*.properties` | `MONGODB_URI` vs `SPRING_DATA_MONGODB_URI`, probes/HPA/PDB absence, image hardening, env-binding | Deploy-fix backlog |
| 8 | **API contract & docs drift** | Postman collection, OpenAPI config, controllers | Keycloak references, multi-service URLs not implemented, MockMvc claim, server-URL `/api` double | Contract-drift list |

### 4.2 Per-subagent prompt contract

Each subagent receives a self-contained brief (no session inheritance) with:

1. **Scope** — exact files/symbols (from GitNexus) it owns; "do not touch other domains."
2. **Mandatory GitNexus use** — call `impact` on every symbol it intends to flag for change; report blast radius + risk level; **stop and surface** any HIGH/CRITICAL finding rather than acting.
3. **Compatibility anchor** — the Lane A target versions from §3, so findings cite the right baseline.
4. **Output schema** — findings as: `{area, severity, evidence(file:line), gitnexus_blast_radius, recommended_action, lane}`.
5. **Constraint** — read-only; no edits; no commits.

### 4.3 Dispatch + integration loop

```
1. Refresh index (§2)                     [blocking, sequential]
2. Dispatch subagents 1..8 in parallel     [concurrent, isolated context]
3. Each returns a findings report
4. Orchestrator merges reports:
     - dedupe overlapping findings
     - cross-check (e.g. subagent 3 PATCH + subagent 6 CORS)
     - order by lane (A before B) and severity
5. Produce unified remediation backlog
```

Parallel-safe because domains share no edit surface; the orchestrator is the only writer (and writes nothing to code here — only to this plan/backlog).

---

## 5. Execution Phases

### Phase 0 — Foundation (sequential, before any review)
- [ ] Re-run `gitnexus analyze`; confirm `status` is fresh.
- [ ] Confirm all queries pass `--repo eccn-management-service`.
- [ ] Snapshot current `./mvnw -q -DskipTests compile` state as the "known-broken" baseline.

### Phase 1 — Parallel review (this plan's core)
- [ ] Dispatch the 8 read-only subagents (§4.1).
- [ ] Collect + merge findings into a backlog.

### Phase 2 — Lane A conservative upgrade (after review, impact-gated)
Safe in-place bumps, each preceded by `impact` and followed by `detect_changes`:
1. Remove direct version pins: Resilience4j, Mockito, Byte Buddy → inherit BOM.
2. Spring Boot 3.4.1 → 3.5.9; Spring Cloud 2024.0.0 → 2025.0.x; Springdoc → 2.8.x.
3. Lombok → 1.18.38; Testcontainers → 1.20.x; Caffeine → 3.2.x; Java Diff Utils → 4.15.
4. Unify MongoDB image (7.0 in compose **and** tests).
5. Remove unused WebFlux + Hystrix references; decide Caffeine/Async.
6. Fix PATCH/CORS, `MONGODB_URI` binding, Actuator exposure.

### Phase 3 — Lane B major migration (fully gated, separate effort)
Only after Lane A is green:
1. Impact-analyze every controller/service/repo for Spring Boot 4.x breakage.
2. Plan Framework 7 / Jakarta EE 11 / auto-config removals.
3. JDK 21 → 25 consideration; re-validate Lombok.
4. Migrate Springdoc 2.x → 3.x.

---

## 6. Guardrails (from AGENTS.md)

- Run `impact` before flagging/editing any symbol; report blast radius; **stop for HIGH/CRITICAL**.
- Run `detect_changes` (or document that only CLI was available) before claiming completion.
- Verify with `./mvnw -q -DskipTests compile` then `./mvnw test` — report exact commands and pass/fail.
- ECCN business outputs are decision-support, not legal determinations; named human compliance approval stays required for final classifications.
