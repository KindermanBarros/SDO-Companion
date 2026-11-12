---
description: Independently review a completed bounded SDO change before developer acceptance.
---
# Workflow: Feature Review

Read [Project contract](../gates/project-contract.md); use a fresh evaluator who did not implement or test the change.

## Entry and baselines

Read the active docs/current summary_vN.md, spec_vN.md, implementation_plan_vN.md and test_plan_vN.md.
For a documentation-only harness change, use its .docs task state and scoped retrospective instead of inventing an application specification.
Missing required approved inputs are findings; do not infer approval from document presence.
Read the relevant source diff, root policy and all ten rule decisions when application delivery is in scope.

## Review

1. Apply android-test-review for application test claims; write test_review_vN.md.
2. Apply android-code-review and [Review checklist](../gates/review-checklist.md); write code_review_vN.md.
3. Run applicable [CI checks](../gates/ci-checks.md), verify actual test counts and declared runtime boundaries.
4. For UI scope, apply ui-verification against approved references; inspect actual screenshots/compare reports, not defect images as goldens.
5. Record verdict, severity, affected locations, required follow-up and blocked evidence. Missing rubric/checkers remain explicit gaps.
6. Present the report for developer review. Do not implement fixes or decide on behalf of the developer that findings are accepted.

The full application rubric remains harness/templates/evaluator-rubric-template.md when present.
Do not invent numerical scores from an absent rubric.
Documentation-only review establishes harness integrity, not Android correctness.
Update task/history records and commit completed validated review artifacts automatically; only human approval moves the task to Done.
