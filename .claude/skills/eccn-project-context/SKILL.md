---
name: eccn-project-context
description: Use when working on ECCN Management Service code, tests, docs, security, deployment, or architecture.
---

# ECCN Project Context

This repo is a Java 21 / Spring Boot 3.4.1 / Maven Wrapper service backed by MongoDB.

Use:
- `tech-stack.md` for stack inventory.
- `prd.md` for product requirements.
- `CLAUDE.md` and `AGENTS.md` for GitNexus and verification rules.

Concrete runtime: one Spring Boot REST API plus MongoDB. Postman includes target multi-service/auth flows that are not fully implemented locally.

Known drift: README Java 17+/MockMvc/GitHub Actions claims do not match current evidence; Docker skips tests; actuator/security/secrets need hardening.
