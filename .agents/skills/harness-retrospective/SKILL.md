---
name: harness-retrospective
description: Repair harness workflow, gate, fixture, or evidence failures without changing app code.
---
# Harness Retrospective

Repair `.agents/`, `harness/templates/`, `.githooks/`, or validator tests when the harness allows false passes, broken links, or token bloat. Never alter application behavior or product scope here.

## Classification
Classify root cause first: `HARNESS_ENVIRONMENT`, `WORKFLOW_GAP`, `TEST_EVIDENCE_GAP`, `SPEC_GAP`, `MODEL_OR_PLATFORM_CAPABILITY`, or `APPLICATION_DEFECT` (stop and route the last two to product/bug workflows).

## Workflow
1. **Orient & reproduce**: Inspect `git status --short`, read the affected instruction/validator file completely, and identify a minimal contract fixture demonstrating the gap.
2. **Define invariant**: State the enforceable rule in one sentence (e.g., [Evidence Invariants](../../rules/testing-strategy.md#evidence-invariants) or [Platform-Bound Evidence](../../rules/testing-runtime-evidence.md#platform-bound-evidence) / [capability matrix contract](../../../harness/templates/platform-capability-matrix-template.md)).
3. **Authorization gate**: Unless the user already requested or authorized editing the harness/workflow files, present the target file list and reason and wait for approval before patching `.agents/`.
4. **Patch & validate**: Make the smallest authoritative change, link canonical docs instead of duplicating rules, and run `.agents/gates/validate-harness.ps1`, `.agents/gates/tests/validate-harness-tests.ps1`, `.agents/gates/tests/scratch-cleanup-tests.ps1` (when cleanup changes), and `git diff --check`.
5. **Record**: Write one single file `docs/changes/harness-retro-<YYYY-MM-DD>-<slug>.md` with incident, classification, invariant, changed files, verification results, and remaining limitations.
