# Copilot Instructions — ECCN Management Service

## Project identity

This repository is a Java 21 Spring Boot 4.0.7 ECCN/export-control management service backed by MongoDB. The concrete runtime is currently one Spring Boot REST API plus MongoDB. Postman files describe target/auth/multi-service flows that are not fully implemented locally.

## Source of truth

- Use `tech-stack.md` for stack inventory.
- Use `prd.md` for product requirements context.
- Use `AGENTS.md` for GitNexus safety rules.
- Verify README/Postman claims against `pom.xml`, source, and config before encoding them as facts.

## Mandatory safety

- Before editing Java symbols, controller endpoints, repository methods, model fields, config beans, or public API contracts, run GitNexus impact analysis when available and report blast radius.
- Stop for approval on HIGH or CRITICAL GitNexus risk.
- Do not rename symbols by plain text replacement; use graph-aware rename when available.
- After edits, run GitNexus change detection when available and targeted Maven validation.

## Development conventions

- Prefer `./mvnw` over system Maven.
- Keep Spring MVC controller → service → repository layering.
- Prefer constructor injection.
- Treat MongoDB transactions carefully; local Compose is a single Mongo container.
- Use Bean Validation and explicit service validation for API inputs.
- Do not invent ECCN legal/regulatory decisions beyond implemented rules and documented requirements.
- Keep secrets out of source. Local `admin/admin` and Mongo `root/secret` are development-only risks.

## Verification

- Compile: `./mvnw -q -DskipTests compile`.
- Full tests: `./mvnw test` once compile is healthy and Mongo/Testcontainers requirements are available.
- Complexity: `python scripts/analyze_complexity.py src/ --recursive --summary` when refactoring risk is relevant.
- Always report exact command output summaries; never claim success without logs.

## ECCN business capability agents

Use capability-specific agents/skills for PRD business workflows:

- `eccn-record-management-capability`
- `eccn-product-classification-capability`
- `eccn-crypto-classification-capability`
- `eccn-glossary-capability`
- `eccn-compliance-documentation-capability`
- `eccn-risk-assessment-capability`
- `eccn-export-control-capability`
- `eccn-classification-workflow-capability`
- `eccn-automated-classification-capability`
- `eccn-enterprise-integration-capability`

All ECCN business outputs are decision support, not final legal/export-control determinations. Require named human compliance approval before final ECCN decisions, `CLASSIFIED` status, compliance approval, report issuance, document archival/deletion, external publication, or high-risk mitigation closure.
