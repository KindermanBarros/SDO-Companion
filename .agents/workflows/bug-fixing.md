---
description: Fix a scoped defect with one diagnosis and relevant regression evidence.
---
# Bug Fixing
Follow [Project contract](../gates/project-contract.md).

1. Read the finding, backlog/capability entry, source and existing evidence. Reuse valid reproduction context.
2. Identify the concrete cause and smallest fix. For behavioral bugs, one relevant RED regression precedes production repair.
3. Cosmetic/docs adjustments use proportional review/build; do not create ceremonial regressions or new visual infrastructure.
4. Record files, risk and checks in the existing task/plan. Reuse explicit authorization; ask only if product scope changes.
5. Apply the minimal fix. Run the original regression GREEN plus checks triggered by the changed boundary, once.
6. For changed platform/IME/lifecycle claims, use the actual runtime boundary; owner build-only/manual verification remains explicitly unverified.
7. Review the diff; use independent review when required by risk or requested. Add unrelated findings to backlog.
8. Update affected docs/backlog and one summary; append history, remove .scratch, check staged scope and commit InDevReview.

Use [CI checks](../gates/ci-checks.md). A missing runtime or unchanged external blocker is not a reason to repeat commands.
Do not fix application behavior during harness maintenance, or claim unexecuted checks passed.
