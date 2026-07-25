---
name: eccn-testing-validation
description: Use before claiming ECCN service changes compile, tests pass, validation is complete, or build failures are fixed.
---

# ECCN Testing and Validation

1. Prefer `./mvnw`.
2. Start narrow, then broaden: targeted tests → `./mvnw -q -DskipTests compile` → `./mvnw test`.
3. Use JUnit 5, Mockito, Spring Boot Test, Reactor Test, and Testcontainers MongoDB patterns already present.
4. If compile fails from baseline issues, quote exact failures and separate baseline from introduced regressions.
5. For refactoring, run `python scripts/analyze_complexity.py` when relevant.
6. Never claim success without fresh command output.
