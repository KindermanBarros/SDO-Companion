---
description: Independently evaluate a completed complex SDO feature against its approved contract.
---
# Workflow: Harness Evaluation

Read [Project contract](../gates/project-contract.md); evaluator must be fresh and have neither implemented nor tested the submitted change.
Use the tracker-linked workspace; missing independent review stays pending.

## Stages

1. **Baselines.** Review tracker/approval consistency and acceptance-to-test mappings. Read spec, all ten Rule Applicability decisions, sprint contract, feature list, design/platform contracts and source diff.
2. **Test review — android-test-review.** Verify Test-ID-to-method/command/report/scenario mappings and real production boundaries. Write test_review_FEATURE_ID.md with rule/test reconciliation.
3. **Code review — android-code-review.** Verify scope, correctness, security and the observed .docs architecture. Run configured static-analysis checks and write code_review_FEATURE_ID.md with rule reconciliation. Do not fix findings inside the review.
4. **Runtime.** Independently establish fresh or valid fingerprint-matched JVM/instrumented results. Required platform/visual/IME/navigation claims need the real declared proof; fake-only evidence is supplemental.
5. **Assess.** Use the agreed review criteria and inspect tracker consistency. Do not claim a mechanical validation or invent a perfect score. Do not invent a substitute perfect score.
6. **Handoff.** Present both reports, rubric, actual gate outcomes and actionable findings. Update .docs and append history; enter InDevReview and commit validated review artifacts automatically.

## Verdict and routing

Every required missing/failing boundary forces Revise/Block, regardless of arithmetic score.
Only a validated 5.0/5 outcome may route To be reviewed to To be human reviewed.
A validated lower score routes to To be fixed and [Harness fix](harness-fix.md).
Review the verdict, required findings and actual evidence before applying the corresponding tracker transition.
Only the developer accepts the work; evaluation never merges, releases or marks a task Done.

## Review evidence

Use the available [CI checks](../gates/ci-checks.md).
Review acceptance mappings, report consistency and declared platform/visual/journey evidence directly; the retired script gates do not apply.
Use the agreed rubric when present and retain findings with severity and source evidence. A missing test result or real boundary is still unproven.
