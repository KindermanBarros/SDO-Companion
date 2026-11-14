---
name: context-management
description: Load minimal session context and triggered rules for the active task.
---
# Context Management

Follow root [AGENTS.md](../../../AGENTS.md), [working-economically.md](../../rules/working-economically.md), and [harness-router](../harness-router/SKILL.md).

## Load order (minimal by default)
1. Read `AGENTS.md` once per session (`testing-strategy.md` is already auto-loaded).
2. Read the requested item in `docs/product/product.md` and resume `.docs/OngoingTasks/<task>_task_state.md`.
3. Load only the active workflow/skill and the rules triggered by the touched boundary:
   - UI / Compose → [compose-rules.md](../../rules/compose-rules.md), [localization-rules.md](../../rules/localization-rules.md)
   - Architecture / layers → [android-architecture.md](../../rules/android-architecture.md), [implementation-rules.md](../../rules/implementation-rules.md)
   - Surface navigation → [navigation-rules.md](../../rules/navigation-rules.md)
   - Room / Firestore / catalogs → [api-contract-rules.md](../../rules/api-contract-rules.md)
   - Security / auth / rules → [android-security.md](../../rules/android-security.md)
   - Runtime / instrumented tests → [testing-runtime-evidence.md](../../rules/testing-runtime-evidence.md)
4. Use `rg` and bounded file reads on the affected package under `app/src/main/java/com/kinderman/sdo/`. Never preload all rules, skills, or historical reports.
