---
name: gitnexus-java-change-safety
description: Use before editing Java classes, methods, repositories, controllers, services, models, config beans, or API contracts in this repository.
---

# GitNexus Java Change Safety

1. Identify exact Java symbols or API contracts affected.
2. Run GitNexus upstream impact analysis when MCP tools are available.
3. Report direct callers, affected flows/processes, and risk level.
4. Stop for approval on HIGH or CRITICAL risk.
5. After edits, run GitNexus change detection and targeted Maven validation.
6. Never claim success without command output.

Fallback when GitNexus MCP tools are unavailable: run `npx gitnexus status`, confirm the index is current, inspect source directly, and state that live impact/detect tools were unavailable.
