---
name: eccn-security-ops
description: Use when reviewing or changing Spring Security, credentials, Actuator, CORS, Docker, Compose, or operational configuration.
---

# ECCN Security and Operations

Check:

- Default `admin/admin` and Mongo `root/secret` are local-only risks.
- Actuator currently exposes all endpoints and health details.
- No explicit `SecurityFilterChain` was found in source.
- Docker builds with `-DskipTests` and runs as default user on a full JDK image.
- CORS omits PATCH while Product API uses PATCH.
- Do not assume Keycloak/OIDC exists just because Postman references bearer auth.
