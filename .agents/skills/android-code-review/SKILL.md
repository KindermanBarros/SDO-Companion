---
name: android-code-review
description: Review Android code for correctness, security, performance, and architecture.
---
# Skill — Android Code Review

Follow [code-review-and-quality](../code-review-and-quality/SKILL.md), [Review checklist](../../gates/review-checklist.md), and [.docs/ARCHITECTURE.md](../../../.docs/ARCHITECTURE.md). For complex multi-slice evaluations, populate [code-review-template.md](../../../harness/templates/code-review-template.md).

## Execution
1. **Scope & provenance**: Record commit, baseline, changed files, and whether checks from [CI checks](../../gates/ci-checks.md) were freshly run or reused by matching fingerprint.
2. **Rule reconciliation**: Check only the rules triggered by the diff ([compose-rules.md](../../rules/compose-rules.md), [localization-rules.md](../../rules/localization-rules.md), [navigation-rules.md](../../rules/navigation-rules.md), [api-contract-rules.md](../../rules/api-contract-rules.md), [android-security.md](../../rules/android-security.md), [implementation-rules.md](../../rules/implementation-rules.md)).
3. **Production traceability**: Verify every in-scope requirement has reachable production wiring (no stubs, `TODO()`, or test-only callbacks) and preserves SDO's manual DI, `AppSurface` navigation, and Standard/Cybergrunge tokens.
4. **Verdict**: Output `APPROVED` or `REVISION REQUIRED` with concrete file/line findings; route out-of-scope observations to the [living backlog](../../../docs/product/product.md#backlog-de-produto).
