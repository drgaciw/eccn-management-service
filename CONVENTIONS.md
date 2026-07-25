# ECCN Management Service Agent Conventions

This file is for harnesses that do not automatically read `.github/`, `.claude/`, `.pi/`, or `.kilodemodes` files.

## Stack

Java 21, Maven Wrapper, Spring Boot 4.0.7, Spring MVC, Spring Data MongoDB, MongoDB, Spring Security, Springdoc OpenAPI, Actuator, Resilience4j, Lombok, Docker/Compose, Postman/Newman.

## Required workflow

- Read `AGENTS.md` and `tech-stack.md` before broad work.
- Use GitNexus for code intelligence when available.
- Before editing Java symbols or public API contracts, run GitNexus impact analysis when available and report blast radius.
- Stop for HIGH/CRITICAL risk.
- Validate with `./mvnw -q -DskipTests compile` or targeted tests; report exact output.
- Do not claim success without fresh verification evidence.

## Known drift

README/Postman contain target or stale claims. Verify against source/config before acting.

## ECCN business capabilities

Use these canonical business-capability agents/skills when work maps to a PRD business process:

- `eccn-record-management-capability` — ECCN CRUD/search/validation/deprecation.
- `eccn-product-classification-capability` — product/version classification status and pending-classification flow.
- `eccn-crypto-classification-capability` — deterministic crypto classification triage.
- `eccn-glossary-capability` — glossary taxonomy, search, bulk import, seed data.
- `eccn-compliance-documentation-capability` — document evidence, versions, diffs, audit/retention.
- `eccn-risk-assessment-capability` — risk scoring, mitigation, follow-up, review dates.
- `eccn-export-control-capability` — jurisdiction classifications, conflicts, compliance requirements.
- `eccn-classification-workflow-capability` — release/product/compliance workflow transitions.
- `eccn-automated-classification-capability` — AI/automated classification evidence and history.
- `eccn-enterprise-integration-capability` — external ECCN validation and enterprise integration stubs.

Business guardrail: outputs are decision support, not final legal/export-control determinations. Require qualified human compliance approval before final ECCN, `CLASSIFIED` status, report issuance, external publication, archival/deletion, or high-risk mitigation closure.
