---
name: implementation-plan
description: Create a concise implementation and test plan before coding.
---
# Skill — Implementation Plan

For bounded fixes, record the 3–7 step plan and selected checks directly in `.docs/OngoingTasks/{task}_task_state.md` per [.docs/TASK_PLANNING.md](../../../.docs/TASK_PLANNING.md). When a full ad-hoc contract is required, generate `docs/current/implementation_plan_v{N}.md` and `docs/current/test_plan_v{N}.md`.

## Execution
1. **Implementation Plan**: Follow [implementation-plan-template.md](../../../harness/templates/implementation-plan-template.md):
   - List files to create/modify/delete across `data/`, `domain/`, `presentation/`, and `ui/`.
   - Detail domain model, repository/Room/Firestore, ViewModel `StateFlow`, and `AppSurface` navigation changes.
   - Link `spec_v{N}.md#rule-applicability` and map every `Required` rule to concrete files and checks.
2. **Test Plan**: Follow [test-plan-template.md](../../../harness/templates/test-plan-template.md):
   - Select the lowest sufficient test layer from [testing-strategy.md](../../rules/testing-strategy.md) and commands from [CI checks](../../gates/ci-checks.md).
   - Separate maintained JVM/`androidTest` regressions from ephemeral `exploratory-qa` runs (stored in `.scratch/qa/<run-id>/`).
3. **Approval**: Present the plan when required by scope and wait for user approval before coding (reuse existing explicit owner authorization when already granted).
