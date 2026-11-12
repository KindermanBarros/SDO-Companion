---
description: Fix an SDO defect using source diagnosis, RED reproduction, an approved plan and GREEN regression.
---
# Workflow: Bug Fixing

Read [Project contract](../gates/project-contract.md); planner owns diagnosis/plan, tester owns reproduction, implementer owns the fix, evaluator reviews independently.

## Entry and preserved evidence

Use for crashes, regressions or behavior that violates the current contract.
Reuse a complete selected exploratory-QA finding: report, build identity, runtime, steps, expected/actual behavior and evidence.
When available, harness/scripts/read-qa-bug-context.py validates that handoff. Missing tooling remains explicit.
Collect only missing runtime evidence; do not replay an already-established journey merely to regenerate its report.
Environment failures route to harness maintenance, and new product behavior routes to requirements.

## Stages

1. **Diagnose — requirement-analysis.** Read current source and affected capability requirements. Record root cause, trigger and the complete ten-row Rule Applicability matrix in the ad-hoc spec/summary. For UI/platform failures, establish the real runtime path using exploratory-qa/agent-device when evidence is missing.
2. **Reproduce — bug-reproduction.** Write and execute the smallest relevant failing test before production changes. Record the named method, command, actual RED result and matching failure cause. A report alone is not RED; unavailable infrastructure is not a relevant failing regression.
3. **Plan — implementation-plan.** Link the RED proof, minimal fix, touched boundaries, test plan and applicable UI/IME contracts. Obtain approval covering the fix plan before source implementation.
4. **Implement — android-implementation.** Apply the scoped root-cause fix; no unrelated refactoring. Require debug build and affected boundary checks.
5. **Verify — android-testing.** The original reproduction and related tests must be GREEN with nonzero executed counts. Remove any temporary Ignore marker; do not suppress failures.
6. **Quality — code-quality-fix.** Apply [CI checks](../gates/ci-checks.md); a required failure or missing checker blocks progression.
7. **Replay and review.** For runtime defects, install the target debug build and repeat the original journey. Retain expected/actual results and mandatory UI screenshots. Route to independent [Feature review](feature-review.md).
8. **Reconcile and hand off.** Reconcile affected product claims in place, update .docs and append history. Enter InDevReview and commit the validated task automatically.

## Required runtime boundaries

Follow [Runtime evidence](../rules/testing-runtime-evidence.md).
Navigation/saved-state/back-stack/post-return regressions require production-entry instrumented journeys with real actions, a return boundary and visible post-return assertions.
IME regressions require shipped-Activity keyboard-open/dismiss tests asserting real insets/window geometry; Content-only fixtures and generic Activities are supplemental.
Keep approved reference images separate from defect screenshots. Visual/rich-text appearance requires declared rendered-output evidence, not model marks.

## Mechanical gates

Run harness/scripts/check-stage-artifacts.sh for bug-fixing requirement-analysis, bug-reproduction, implementation-plan and testing.
For declared visual, journey, product and platform claims, also run the triggered validators in [CI checks](../gates/ci-checks.md).
Missing scripts, runtime prerequisites or matching test reports remain Blocked; do not replace required proof with manual assertions.
If the defect cannot be reproduced, report that gap before attempting the production fix.
