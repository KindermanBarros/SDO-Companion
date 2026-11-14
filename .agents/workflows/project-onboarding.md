---
description: Refresh the configured SDO harness or explicitly plan an application adoption repair.
---
# Workflow: Project Onboarding
Follow [Project contract](../gates/project-contract.md). SDO already has a populated `.docs/` baseline; no setup prompt is used.

## Documentation refresh
1. Inspect root policy, affected `.docs/` files, Gradle/manifest/source facts, and `git status`.
2. Resume/create the task state, update only authorized files (`[SEALED]` files require explicit owner request), run `validate-harness.ps1` from [CI checks](../gates/ci-checks.md), remove `.scratch`, append history, and commit in `InDevReview`.

## Application adoption repair
1. Inspect `harness-project.json` and `docs/current/onboarding-summary.md` against current source. Treat manual DI, custom `AppSurface` navigation, and `AuthRepository` `Activity` usage as explicit baseline observations requiring owner-approved scope before migration.
2. Run triggered Gradle checks from [CI checks](../gates/ci-checks.md) and record actual test counts and remaining quality/coverage limitations honestly.
