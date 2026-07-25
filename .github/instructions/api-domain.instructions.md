---
applyTo: "src/main/java/**/controller/**/*.java,src/main/java/**/service/**/*.java,src/main/java/**/model/**/*.java,postman/**,README.md,prd.md"
---

# ECCN API and Domain Instructions

- Implemented APIs: ECCN (`/api/v1/eccn`), Product (`/api/products`), Glossary (`/api/v1/glossary`), Crypto Classification (`/api/crypto-classification`), Health (`/api/health`).
- Postman references future auth/classification/risk/compliance services; verify before treating those endpoints as implemented.
- Keep OpenAPI annotations/examples accurate and avoid double-prefix confusion with `/api` server URL.
- Preserve ECCN validation semantics unless the user explicitly requests a domain-rule change.
- Escalate legal/regulatory ambiguity; do not invent export-control rules.
- When changing endpoint contracts, update tests/docs/Postman only if in scope and report compatibility impact.
