---
applyTo: "src/test/**/*.java,src/test/resources/**,pom.xml,scripts/**"
---

# Testing and Validation Instructions

- Use JUnit 5, Mockito, Spring Boot Test, Reactor Test, and Testcontainers MongoDB as appropriate.
- Do not claim MockMvc coverage unless MockMvc is actually added; current stack review found no MockMvc usage.
- Prefer narrow validation first, then broader Maven checks.
- Compile command: `./mvnw -q -DskipTests compile`.
- Full test command: `./mvnw test` once compile is healthy.
- For Java complexity/refactoring work, use `python scripts/analyze_complexity.py`.
- If validation fails because of known baseline compile errors, report exact output and separate baseline failures from new regressions.
