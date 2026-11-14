---
description: Independently evaluate a completed complex SDO feature against its approved contract.
---
# Workflow: Harness Evaluation
Follow [Project contract](../gates/project-contract.md) and [Review checklist](../gates/review-checklist.md). For complex multi-slice features, use an independent evaluator that did not author the implementation.

1. **Baseline**: Read the workspace `spec.md`, `sprint-contract.md`, `feature_list.json`, and diff. Do not modify implementation code during evaluation.
2. **Test review (`android-test-review`)**: Verify FR/AC-to-test traceability, production triggers, assertions, and runtime/coverage evidence; write `test_review_<FEATURE_ID>.md`.
3. **Code review (`android-code-review`)**: Verify architecture boundaries, security, localization, Compose rules, and static analysis from [CI checks](../gates/ci-checks.md); write `code_review_<FEATURE_ID>.md`.
4. **Verdict & handoff**: Any missing required boundary or failing check routes to `To be fixed` ([Harness Fix](harness-fix.md)); a clean pass routes to `To be human reviewed`. Remove `.scratch`, append `.docs` history, and commit review artifacts in `InDevReview`.
