<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **eccn-management-service** (1650 symbols, 3207 relationships, 133 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/eccn-management-service/context` | Codebase overview, check index freshness |
| `gitnexus://repo/eccn-management-service/clusters` | All functional areas |
| `gitnexus://repo/eccn-management-service/processes` | All execution flows |
| `gitnexus://repo/eccn-management-service/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->

# ECCN Management Service — Project Agent Rules

## Project Stack

- Java 21, Maven Wrapper, Spring Boot 4.0.7, Spring MVC, Spring Data MongoDB, MongoDB, Spring Security, Springdoc OpenAPI, Actuator, Resilience4j, Lombok, Docker/Compose, Postman/Newman.
- Use `tech-stack.md` as the local stack inventory and verify claims against source/config before acting.
- The concrete runtime is currently one Spring Boot API plus MongoDB. Postman includes target multi-service/auth flows that are not fully implemented locally.

## Mandatory Change Safety

- Prefer GitNexus `query`/`context` for unfamiliar flows before grepping broadly.
- Before editing Java symbols, APIs, repository methods, model fields, config beans, or public endpoint contracts, satisfy the GitNexus impact rule above and report blast radius.
- Stop for approval if GitNexus reports HIGH or CRITICAL risk.
- Before completion after edits, run GitNexus `detect_changes()` when the harness exposes it; otherwise document that only CLI/status was available.

## Verification Defaults

- Prefer `./mvnw` over system `mvn`.
- Compile check: `./mvnw -q -DskipTests compile`.
- Full test check: `./mvnw test` once compile is healthy and Mongo/Testcontainers requirements are available.
- Complexity check when refactoring Java: `python scripts/analyze_complexity.py src/ --recursive --summary` or a targeted file command.
- Always report exact commands and pass/fail output; never claim builds/tests pass without fresh evidence.

## Known Project Drift / Risks

- README says Java 17+, MockMvc, and GitHub Actions; `pom.xml`/Docker use Java 21, no MockMvc usage or `.github/workflows` were found.
- Docker build skips tests.
- `application.properties` contains local default credentials and exposes all actuator endpoints/health details.
- CORS omits PATCH while Product API uses PATCH.
- Existing `.kilocodemodes` had stale TypeScript/actor-system language; use ECCN-specific modes instead.

## ECCN Business Capability Agents

Use these project agents/skills for PRD business processes: `eccn-record-management-capability`, `eccn-product-classification-capability`, `eccn-crypto-classification-capability`, `eccn-glossary-capability`, `eccn-compliance-documentation-capability`, `eccn-risk-assessment-capability`, `eccn-export-control-capability`, `eccn-classification-workflow-capability`, `eccn-automated-classification-capability`, and `eccn-enterprise-integration-capability`.

All ECCN business outputs are decision support, not legal advice or final export-control determinations. Require named human compliance approval before final ECCN decisions, `CLASSIFIED` status, compliance approval, report issuance, document archival/deletion, external publication, or high-risk mitigation closure.

## Local Spring Boot Specialized Subagents & Skills

### Subagents
- `springboot-architecture-reviewer`: Audits REST controller status codes (`201 Created`, `204 No Content`), `@Transactional(readOnly = true)` transaction scoping, record DTO validation, and RFC 7807 problem details.
- `springboot-mongodb-specialist`: Audits Spring Data MongoDB repositories, `@Indexed` compound indexes, and MongoDB regex query performance.
- `springboot-resilience-observability-agent`: Audits Resilience4j `@CircuitBreaker`/`@Retry` fallbacks, `ThreadPoolTaskExecutor` async configuration, Actuator metrics, and Logback MDC request tracing.

### Project Skills
- `eccn-springboot-architecture`: `.gemini/skills/eccn-springboot-architecture/SKILL.md` (Spring Boot 4.0 REST, transactions, record DTOs, RFC 7807).
- `eccn-mongo-repository-patterns`: `.gemini/skills/eccn-mongo-repository-patterns/SKILL.md` (Spring Data MongoDB queries and indexing).
- `eccn-resilience-circuitbreaker`: `.gemini/skills/eccn-resilience-circuitbreaker/SKILL.md` (Resilience4j, thread pools, MDC tracing).

## Local Spring AI Specialized Subagents & Skills

### Subagents
- `spring-ai-chat-client-specialist`: Audits `ChatClient.Builder` fluent API usage, system prompts, structured JSON output converters (`BeanOutputConverter`), and multi-provider fallback.
- `spring-ai-tool-calling-agent`: Audits Spring AI `@Bean` function definitions, agentic tool registration in `EccnTools`, and schema validation.
- `spring-ai-evaluator-agent`: Audits multi-LLM consensus deliberation (`CouncilOrchestrator`), model weighting, voting, and human compliance disclaimers.

### Project Skills
- `eccn-spring-ai-chatclient`: `.gemini/skills/eccn-spring-ai-chatclient/SKILL.md` (ChatClient.Builder, prompts, structured outputs).
- `eccn-spring-ai-function-calling`: `.gemini/skills/eccn-spring-ai-function-calling/SKILL.md` (@Bean tools, EccnTools, function calling).
- `eccn-spring-ai-council-orchestration`: `.gemini/skills/eccn-spring-ai-council-orchestration/SKILL.md` (Multi-LLM deliberation, CouncilOrchestrator).
