---
applyTo: "prd.md,src/main/java/**/*.java,src/test/java/**/*.java,postman/**,README.md"
---

# ECCN Business Guardrails

## Common ECCN business rules

- Outputs are decision support, not legal advice or final export-control determinations.
- Require qualified human compliance approval before final ECCN, `CLASSIFIED` status, compliance approval, report issuance, archival/deletion, external publication, or high-risk mitigation closure.
- Do not override explicit compliance-manager decisions; raise conflicts with evidence.
- Every recommendation must include inputs, source references, rationale, assumptions, uncertainty, and missing-data checklist.
- Preserve auditability: actor, role, timestamp, action, status transition, before/after summary, evidence references, rationale, approver, and correlation/request ID.
- Verify implemented-vs-target status before recommending APIs or automation.
- Minimize sensitive evidence; do not expose proprietary source, credentials, customer identifiers, or secrets beyond approved boundaries.


Escalate high-risk, low-confidence, restricted-end-use, high-risk-user, jurisdiction-conflict, crypto-change, external-validation-conflict, or missing-data cases to compliance review before state changes.
