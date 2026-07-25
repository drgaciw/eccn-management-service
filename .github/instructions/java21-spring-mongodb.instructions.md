---
applyTo: "**/*.java,pom.xml,src/main/resources/**/*.properties"
---

# Java 21 / Spring Boot / MongoDB Instructions

- Baseline is Java 21, Spring Boot 3.4.1, Maven Wrapper, Spring MVC, Spring Data MongoDB, Lombok, and MongoDB.
- Keep controllers thin; put business rules in services and persistence concerns in repositories.
- Prefer Spring MVC patterns already present. Do not introduce WebFlux/reactive code unless explicitly requested.
- Use constructor injection and avoid field injection.
- Preserve package root `com.aciworldwide.eccn_management_service`.
- Before editing Java symbols, follow `AGENTS.md` GitNexus impact-analysis requirements.
- Add or update JUnit 5 / Mockito / Spring Boot Test / Testcontainers tests when behavior changes.
- Verify Lombok-generated methods/build behavior with Maven because Lombok is a known compile-risk area.
