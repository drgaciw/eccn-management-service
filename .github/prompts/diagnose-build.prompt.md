---
description: Diagnose compile/test failures in the ECCN Spring Boot service.
---

Read `tech-stack.md`, `AGENTS.md`, and current `git status --short`. Run the narrowest useful Maven command, usually `./mvnw -q -DskipTests compile`. Classify failures as baseline vs introduced by current changes. If editing Java symbols, perform GitNexus impact analysis first. Return exact command output summary, suspected root causes, proposed smallest fixes, and validation plan.
