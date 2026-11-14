---
name: android-test-review
description: Review Android test traceability, boundaries, assertions, and coverage.
---
# Skill — Android Test Review

Follow [testing-strategy.md](../../rules/testing-strategy.md), [testing-practices.md](../../rules/testing-practices.md), and [Review checklist](../../gates/review-checklist.md). For complex-feature evaluations, populate [test-review-template.md](../../../harness/templates/test-review-template.md).

## Review checklist
1. **Provenance**: Record commit, changed files, test commands, exit codes, and whether results were freshly executed or reused by matching fingerprint.
2. **Traceability**: Verify that every in-scope requirement, acceptance criterion, and distinct outcome (happy path, fallback, error, migration) maps to a real test method that triggers the production entry point and asserts the observable outcome.
3. **Assertion & boundary quality**: Reject setter-only tests, `assertTrue(true)`, test-only callback wiring, inline wildcard/FQN imports, or fake-only proofs for platform/Room migration/navigation claims ([testing-runtime-evidence.md](../../rules/testing-runtime-evidence.md)).
4. **Coverage & regressions**: Check that bug fixes have a verified RED→GREEN regression and review Kover coverage against thresholds (80% overall, 90% new ViewModels/use cases) when applicable.
5. **Verdict**: Output `APPROVED` or `REVISION REQUIRED` with concrete file/method findings.
