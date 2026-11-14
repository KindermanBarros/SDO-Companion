---
name: test-design
description: Select post-merge mobile acceptance test cases and exploratory risk targets.
---
# Feature Acceptance Test Design

Use for Stage 2 post-merge acceptance in [Feature Acceptance](../../workflows/feature-acceptance.md). Do not replace pre-merge test planning or execute tests here.

## Workflow
1. **Evidence sources**: Read approved feature requirements/ACs (`docs/changes/` or `docs/product/requirements/`), `UI_contract.md` (if UI), and existing regression tests. Never invent ACs from code.
2. **Representative cases**: Map every in-scope AC to a deterministic acceptance case or an explicit coverage gap. Assign priority and release criticality before execution, respecting [API evidence limits](../api-mocking/SKILL.md#ownership-and-evidence) and [ephemeral scope](../exploratory-qa/SKILL.md#target-and-scenario) ([artifact layout](../exploratory-qa/references/artifact-layout.md)). Classify each case's API mode (`real`, `hybrid`, or `mocked`).
3. **Produce plan**: Write `.scratch/qa/<suite-id>/test_plan_v{N}.md` using [test-plan-template.md](../../../harness/templates/test-plan-template.md) (see [examples/node-sharing.md](examples/node-sharing.md)). Include scope/sources, AC coverage table, execution order, numbered step-by-step test cases with explicit oracles, coverage gaps, and a separate `Exploratory risk targets` section with a time/action budget.
4. **Handoff**: Return the plan path and gaps for [plan review](../../workflows/feature-acceptance.md#1-plan--test-design) before [exploratory-qa](../exploratory-qa/SKILL.md) executes.
