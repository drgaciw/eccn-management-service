---
applyTo: "src/main/resources/**,src/main/java/**/config/**,src/main/java/**/controller/**,Dockerfile,compose.yaml,postman/**"
---

# Security, Compliance, and Operations Instructions

- Treat ECCN/export-control data as compliance-sensitive.
- Do not commit production secrets or credentials.
- Current risks to consider: default `admin/admin`, Mongo `root/secret`, broad actuator exposure, health details always shown, no explicit `SecurityFilterChain`, CORS missing PATCH, Docker tests skipped.
- Prefer canonical Spring Boot env vars such as `SPRING_DATA_MONGODB_URI` unless custom binding is added.
- Restrict actuator exposure by profile and authorization in production guidance.
- Do not assume Keycloak/OIDC exists just because Postman references a bearer-token flow.
- Validate security/ops changes with focused tests, config checks, or documented manual verification.
