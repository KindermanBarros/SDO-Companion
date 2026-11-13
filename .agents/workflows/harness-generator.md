---
description: Implement one approved complex SDO feature slice without widening its contract.
---
# Workflow: Harness Generator

Read [Project contract](../gates/project-contract.md). Use implementer, tester and fresh evaluator for their stages.
Entry requires approved feature_list.json and sprint-contract.md from [Harness planning](harness-planning.md).
If the tracker says To be fixed, use [Harness fix](harness-fix.md); do not select another slice.

## Execution

1. **Orient — feature-orient.** Validate lifecycle, select the tracker-linked workspace/slice and read spec, contracts, rule decisions, approved UI states and platform owners. Record their paths/hashes in summary_FEATURE_ID.md. Review the selected contract and its approval provenance before implementation.
2. **Runtime preparation.** Inspect adb devices and bind required runtime evidence to one declared serial/build. Device absence blocks runtime stages, not unrelated documentation investigation.
3. **Baseline.** Run available static-analysis, debug-build and JVM checks from [CI checks](../gates/ci-checks.md). A failed prerequisite blocks the stage; repair unrelated baseline debt only under authorized scope.
4. **Implement — android-implementation.** Implement only the selected behavior and its approved dependencies. Follow the adopted persona's TDD rules; keep fixtures out of production selection logic.
5. **Test — android-testing.** Implement all selected acceptance rows with named methods and nonzero passing reports. Assert real platform boundaries for owning slices, production-entry navigation/lifecycle journeys and declared visual comparisons. Run affected registered journeys.
6. **Quality — code-quality-fix.** Require the configured formatting/static-analysis/Lint commands and reviewed coverage results.
7. **State.** Attach successful evidence per Test ID before marking a slice passing. Reuse receipts only when source/config/command/runtime fingerprints still match. After three unsuccessful retries, leave the affected work blocked.
8. **Handoff.** Reconcile affected product claims, update .docs/task state, append history, and create the workspace session-handoff record. Enter InDevReview and commit the validated task automatically.
9. **Install.** For declared UI/platform/installation scope, install to the selected target runtime, capture required screenshots and record identity/result. No runtime is Blocked, not successful installation.

## Verification evidence

Use available [CI commands](../gates/ci-checks.md) and the selected slice's actual test methods.
Review acceptance-ID/method/scenario mappings, platform/journey ownership and successful reports directly.
Before reuse, compare source/config/command/runtime fingerprints; record that review rather than claiming a validator ran.
After source/build/profile changes, inspect Gradle, manifest and source against the profile and update observed documentation within scope.
Retired context-index, source, lifecycle, stage, receipt, platform/visual and other legacy script gates are removed.

## Lifecycle

While slices remain, keep the feature In Progress unless blocked.
Only after every slice and required gate passes, route to To be reviewed and [Harness evaluation](harness-evaluation.md).
Never advance directly to human-reviewed or Complete; independent review and developer acceptance remain pending.
No automatic push, branch creation, PR, merge or release.
