---
description: Implement one approved complex SDO feature slice without widening its contract.
---
# Workflow: Harness Generator
Follow [Project contract](../gates/project-contract.md). Requires approved `feature_list.json` and `sprint-contract.md` from [Harness Planning](harness-planning.md). If tracker status is `To be fixed`, route to [Harness Fix](harness-fix.md).

1. **Orient (`feature-orient`)**: Select the highest-priority `not_started` slice in `feature_list.json`, read only its mapped spec/contract rows, and mark it `in_progress`.
2. **Implement (`android-implementation`)**: Implement only the slice's approved Data, Domain, and UI changes following `.docs/ARCHITECTURE.md` and `.docs/CODE_STANDARDS.md`.
3. **Test & verify (`android-testing`, `code-quality-fix`)**: Implement mapped acceptance tests and run triggered [CI checks](../gates/ci-checks.md) once. Reuse matching evidence by fingerprint; missing required runtime is `Blocked`, never fabricated as passing.
4. **Handoff**: Attach command/count/result evidence to the workspace summary, reconcile affected product docs/backlog, remove `.scratch`, append `.docs` history, and create a scoped signed-off commit in `InDevReview`. When all slices pass, set tracker status to `To be reviewed` for [Harness Evaluation](harness-evaluation.md).
