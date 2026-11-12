---
description: Independently evaluate a completed complex SDO feature against its approved contract.
---
# Workflow: Harness Evaluation

Read [Project contract](../gates/project-contract.md); evaluator must be fresh and have neither implemented nor tested the submitted change.
Use the tracker-linked workspace; missing independent review stays pending.

## Stages

1. **Baselines.** Validate lifecycle and acceptance traceability. Read spec, all ten Rule Applicability decisions, sprint contract, feature list, design/platform contracts and source diff.
2. **Test review — android-test-review.** Verify Test-ID-to-method/command/report/scenario mappings and real production boundaries. Write test_review_FEATURE_ID.md with rule/test reconciliation.
3. **Code review — android-code-review.** Verify scope, correctness, security and the observed .docs architecture. Run full-source checks and write code_review_FEATURE_ID.md with rule reconciliation. Do not fix findings inside the review.
4. **Runtime.** Independently establish fresh or valid fingerprint-matched JVM/instrumented results. Required platform/visual/IME/navigation claims need the real declared proof; fake-only evidence is supplemental.
5. **Assess.** Use the required evaluator rubric and lifecycle validator. Missing scoring/template/checker prerequisites block a validated score/transition. Do not invent a substitute perfect score.
6. **Handoff.** Present both reports, rubric, actual gate outcomes and actionable findings. Update .docs and append history; enter InDevReview and commit validated review artifacts automatically.

## Verdict and routing

Every required missing/failing boundary forces Revise/Block, regardless of arithmetic score.
Only a validated 5.0/5 outcome may route To be reviewed to To be human reviewed.
A validated lower score routes to To be fixed and [Harness fix](harness-fix.md).
Run check-evaluation-fix-contract.sh --evaluation and check-feature-lifecycle.sh before claiming the transition valid.
Only the developer accepts the work; evaluation never merges, releases or marks a task Done.

## Required scripts and rubric

Use harness/scripts/check-acceptance-test-traceability.sh --evaluate, full-source rules, platform/visual/rendered-output/journey validators and applicable [CI checks](../gates/ci-checks.md).
The scoring authority is harness/templates/evaluator-rubric-template.md when available.
These scripts/templates are currently absent. Record the exact prerequisite gap; structural documentation validation cannot replace them.
