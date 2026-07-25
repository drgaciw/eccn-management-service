---
description: Review and harden Spring Security, Actuator, secrets, Docker, or Compose config.
---

Start from current risks in `tech-stack.md`: default credentials, broad actuator exposure, no explicit SecurityFilterChain, Docker tests skipped, CORS PATCH gap. Do not assume Keycloak/OIDC is implemented. Propose smallest safe changes and validation commands; ask for product/security decisions before selecting an auth provider.
