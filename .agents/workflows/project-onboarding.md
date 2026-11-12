---
description: Refresh the configured SDO harness or explicitly plan an application adoption repair.
---
# Workflow: Project Onboarding

Read [Project contract](../gates/project-contract.md). SDO already has an Android foundation and a populated .docs baseline. No setup prompt is required.

## Documentation refresh

1. Inspect root policy, affected .docs files, module/build/manifest/source evidence and git status.
2. Create/resume the .docs task state. Keep observations distinct from desired architecture and historical test results.
3. Resolve missing product decisions with the owner; record explicit Pending/None when appropriate.
4. Update only the authorized baseline/instructions; sealed files require an explicit owner request.
5. Run the structural command in [CI checks](../gates/ci-checks.md), append history and hand off as InDevReview.
6. Remove disposable setup inputs only when the owner requests it; keep architecture, approvals, product contracts and previous test evidence.

## Full application adoption repair

This is separate from documentation refresh. Use project-onboarding and the existing docs/current onboarding plan/spec/test records as evidence; verify that any recorded approval still covers the proposed scope.

- Inspect harness-project.json and docs/architecture/architecture-map.md against current Gradle and source.
- The observed map is not policy and is not fresh merely because it exists.
- The existing application uses manual DI, custom navigation and an Android-dependent auth contract. Propose bounded repairs or explicit decisions before changing them.
- Do not relabel a failing existing application as a new empty project.
- Approval of documentation cleanup does not authorize source, schema, build, Firebase or architecture changes.

When the required tooling exists, run python3 harness/scripts/project_onboarding.py discover, reconcile the profile, and run check/verify.
Required verify evidence includes matching approved inputs, fresh source fingerprints, lifecycle/source checks, build and nonzero passing test reports.
The scripts are currently absent; full application onboarding remains blocked until the missing prerequisites are resolved within authorized scope.
Do not delete historical approval, source-map or runtime evidence as setup debris.
