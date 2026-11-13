---
description: Resolve complex-feature evaluator findings and preserve their evidence and status.
---
# Workflow: Harness Fix

Read [Project contract](../gates/project-contract.md).
Use implementer for fixes, tester for verification and a fresh evaluator for independent review.
Entry requires tracker status To be fixed and the workspace's code/test review reports; do not select a new slice or duplicate the approved plan.

## Fix status policy

Every finding remains visible in its original report and also appears in the consolidated fix list.
Use Fixed only after its verification passes, Unresolved for a failure/missing prerequisite, and Won't fix only for an explicitly approved false-positive disposition.
Each code finding needs a Fix Status line; each test review row needs Fix Status; update the report verdict/fix summary consistently.
Use file/diff references before an automatic commit exists, then record its hash if created.

## Pipeline

1. **Orient.** Validate lifecycle and read sprint contract, platform/visual contracts, evaluator rubric, code/test reviews and previous handoff. Build a deduplicated fix list.
2. **Baseline.** Inspect required runtime and run source/build/JVM gates. A failing unrelated baseline is a blocker, not authorization to repair arbitrary application code.
3. **Fix.** Apply android-implementation/android-testing only to the reported root causes. Respect the selected persona's testing policy. Do not weaken checks, add suppressions or enlarge scope.
4. **Re-verify.** Run each affected acceptance command and all required [CI checks](../gates/ci-checks.md). Preserve actual command, exit status, counts and reports. Reused receipts require matching source/config/command/runtime hashes.
5. **Reconcile.** Update every finding in-report and summary; failed proof stays Unresolved. Reconcile all ten rule decisions and product claims against actual verification.
6. **Handoff.** Review in-report statuses and evidence consistency before progression. Only a complete successful fix pass may route to To be human reviewed. Append .docs history, enter InDevReview and commit the validated task automatically.
7. **Runtime.** Install to the declared target when a finding affects UI/platform behavior; retain required screenshots and replay evidence.

## Verification evidence

Use available [CI commands](../gates/ci-checks.md) and each affected acceptance-test selector.
Review per-finding status, matching test reports, source/config/command/runtime fingerprints and platform/visual/journey claims directly.
Retired script-backed gates do not apply. Required runtime failures and unverified test assertions remain non-passing.

## Final report

Preserve per-finding status, verification references, unresolved risks and the human-review handoff.
Do not mark a required failure fixed, convert it into a warning or claim complete application readiness.
