---
name: slice-planning
description: Decompose complex requirements into vertical slices and a sprint contract.
---
# Skill — Slice Planning

Use only when a complex feature spans multiple logical areas and uses [Harness Planning](../../workflows/harness-planning.md). Bounded tasks use a short task plan in `.docs/OngoingTasks/`.

## Inputs & templates
Read the approved `spec.md` (and `UI_contract.md` when UI is in scope), then follow:
- [sprint-contract-template.md](../../../harness/templates/sprint-contract-template.md)
- [feature_list_template.json](../../../harness/templates/feature_list_template.json)
- [progress-template.md](../../../harness/templates/progress-template.md)
- [platform-capability-matrix-template.md](../../../harness/templates/platform-capability-matrix-template.md) (only when `platform_validation.required` is `true`)

## Execution rules
1. **Spec Coverage Matrix**: Map every `FR-*`, `AC-*`, edge case, and distinct outcome (happy path, fallback, error, migration/compatibility) to one primary user story (`US-1`, `US-2`, ...) and one primary acceptance Test ID.
2. **Vertical slices only**: Every slice must deliver an observable behavior reachable from an existing production entry point and leave the build green. Merge horizontal helper-only slices into the first slice that exposes them.
3. **1:1 cross-reference**: Every `US-*` in `sprint-contract.md` must match exactly one `features[].id` in `feature_list.json` with no orphans or duplicates.
4. **Journey & platform declarations**:
   - Mirror `production_journey` (`required`, `reason`, entry point, `app/src/androidTest/` target, actions, return boundary, assertion) between `sprint-contract.md` and `feature_list.json`.
   - Include root `platform_validation` in `feature_list.json` (`required: true` with capability matrix and real boundary test IDs, or `required: false` with a reason and no matrix file).
   - If UI changes exist, assign `requires_visual_verification: true` to the final user-reachable slice only.
5. **Approval**: Present `sprint-contract.md` and `feature_list.json` (with Slice 1 set to `in_progress` and others `not_started`) and obtain user approval before [Harness Generator](../../workflows/harness-generator.md).
