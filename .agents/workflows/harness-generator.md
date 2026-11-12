---
description: Implement one approved complex SDO feature slice without widening its contract.
---
# Workflow: Harness Generator

Read [Project contract](../gates/project-contract.md). Use implementer, tester and fresh evaluator for their stages.
Entry requires approved feature_list.json and sprint-contract.md from [Harness planning](harness-planning.md).
If the tracker says To be fixed, use [Harness fix](harness-fix.md); do not select another slice.

## Execution

1. **Orient — feature-orient.** Validate lifecycle, select the tracker-linked workspace/slice and read spec, contracts, rule decisions, approved UI states and platform owners. Record their paths/hashes in summary_FEATURE_ID.md. Validate the orient stage and context index before implementation.
2. **Runtime preparation.** Inspect adb devices and bind required runtime evidence to one declared serial/build. Device absence blocks runtime stages, not unrelated documentation investigation.
3. **Baseline.** Run required full-source, debug-build and JVM checks from [CI checks](../gates/ci-checks.md). A failed prerequisite blocks the stage; repair unrelated baseline debt only under authorized scope.
4. **Implement — android-implementation.** Implement only the selected behavior and its approved dependencies. Follow the adopted persona's TDD rules; keep fixtures out of production selection logic.
5. **Test — android-testing.** Implement all selected acceptance rows with named methods and nonzero passing reports. Assert real platform boundaries for owning slices, production-entry navigation/lifecycle journeys and declared visual comparisons. Run affected registered journeys.
6. **Quality — code-quality-fix.** Require the full repository source bundle, formatting/static analysis/Lint and coverage. Never replace full-source validation with a changed-file scan.
7. **State.** Attach successful evidence per Test ID before marking a slice passing. Reuse receipts only when source/config/command/runtime fingerprints still match. After three unsuccessful retries, leave the affected work blocked.
8. **Handoff.** Reconcile affected product claims, update .docs/task state, append history, and create the workspace session-handoff record. Enter InDevReview and commit the validated task automatically.
9. **Install.** For declared UI/platform/installation scope, install to the selected target runtime, capture required screenshots and record identity/result. No runtime is Blocked, not successful installation.

## Required mechanical evidence

All paths below are under harness/scripts; absence remains a hard prerequisite gap.

- check-feature-lifecycle.sh and print-context-index.sh for validated selection/orientation.
- check-stage-artifacts.sh harness-generator orient with workspace and slice.
- check-full-source-rules.sh and the triggered commands in CI checks.
- check-acceptance-test-traceability.sh with --test and selected slice.
- check-platform-evidence.sh --evaluate --slice for the owning boundary.
- check-visual-evidence-contract.sh and check-rendered-output-contract.sh for declared appearance claims.
- check-journey-registry.sh for affected journeys and any new approved production journey.
- project_onboarding.py discover --refresh and check after source/build/profile changes.
- check-product-docs.py and check-harness-metrics.sh for the applicable records.

## Lifecycle

While slices remain, keep the feature In Progress unless blocked.
Only after every slice and required gate passes, route to To be reviewed and [Harness evaluation](harness-evaluation.md).
Never advance directly to human-reviewed or Complete; independent review and developer acceptance remain pending.
No automatic push, branch creation, PR, merge or release.
