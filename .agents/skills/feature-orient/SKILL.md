---
name: feature-orient
description: Orient to the active sprint contract, slice, and task state.
---
# Skill — Feature Orient

Used at the start of [Harness Generator](../../workflows/harness-generator.md) for complex multi-slice features.

## Execution
1. **Validate tracker**: Inspect the Harness Feature Tracker in [product.md](../../../docs/product/product.md) and confirm the active `$FEATURE_DIR` workspace has approved `spec.md`, `sprint-contract.md`, and `feature_list.json`.
2. **Select slice**: Pick the highest-priority `not_started` slice in `feature_list.json`, mark it `in_progress`, and read only its mapped user story (`US-*`), acceptance rows, and triggered rules.
3. **Check prior notes**: Read only the latest entry in `$FEATURE_DIR/progress.md` (and `evaluator-rubric.md` or `docs/knowledge/` only if relevant to the selected slice).
4. **Initialize summary**: Create or resume `$FEATURE_DIR/summary_<feature_id>.md` using [summary-template.md](../../../harness/templates/summary-template.md) and [harness-generator.md](../../../harness/templates/summary-profiles/harness-generator.md), citing contract paths without copying their tables.
