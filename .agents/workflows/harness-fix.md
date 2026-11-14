---
description: Resolve complex-feature evaluator findings and preserve their evidence and status.
---
# Workflow: Harness Fix
Follow [Project contract](../gates/project-contract.md). Entry requires tracker status `To be fixed` and existing review reports in the feature workspace.

1. **Orient**: Build a deduplicated fix list from `code_review_*.md` and `test_review_*.md`. Keep every finding visible in its original report with a `Fix Status` (`Fixed`, `Unresolved`, or owner-approved `Won't fix`).
2. **Fix**: Apply minimal root-cause fixes in production or test files using `android-implementation` and `android-testing` without widening scope or suppressing rules.
3. **Re-verify**: Run only the affected acceptance test selectors and triggered [CI checks](../gates/ci-checks.md); reuse valid unchanged evidence by fingerprint.
4. **Handoff**: Update finding statuses and summary evidence. When all required findings are resolved, route tracker status to `To be human reviewed`; otherwise keep `To be fixed` / `Blocked`. Remove `.scratch`, append `.docs` history, and create a scoped signed-off commit in `InDevReview`.
