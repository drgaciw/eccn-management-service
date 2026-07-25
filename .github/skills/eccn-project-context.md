---
name: eccn-project-context
description: Use when working on the ECCN Management Service code, docs, tests, security, or deployment files.
---

# ECCN Project Context

- Java 21, Maven Wrapper, Spring Boot 3.4.1.
- Spring MVC REST API backed by Spring Data MongoDB and MongoDB.
- Uses Spring Security defaults, Springdoc OpenAPI, Actuator, Resilience4j, Lombok, Docker/Compose, Postman/Newman, and Python complexity tooling.
- Concrete runtime is one service + MongoDB; multi-service/auth flows in Postman are target/partial.
- Read `tech-stack.md`, `prd.md`, and `AGENTS.md` before broad work.
