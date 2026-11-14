---
name: project-onboarding
description: Refresh SDO harness documentation or reconcile application adoption baseline.
---
# Project Onboarding

Procedures for [Workflow: Project Onboarding](../../workflows/project-onboarding.md). `SDO-Companion` already has an established baseline in `.docs/` and `harness-project.json`.

## Procedures
1. **Discover & reconcile**: Inspect `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, and `app/src/` directly. Keep observations distinct from policy.
2. **Profile maintenance**: Keep `harness-project.json` module roots, layers (`data`, `domain`, `ui`/`presentation`), and Gradle commands aligned with [.docs/VALIDATION.md](../../../.docs/VALIDATION.md) and [CI checks](../../gates/ci-checks.md). Retain unresolved adoption items in `unknowns` until an owner-approved task resolves them.
3. **Verify**: Run `.agents/gates/validate-harness.ps1` for documentation refreshes, and run triggered Gradle checks from [CI checks](../../gates/ci-checks.md) when application adoption repairs are in scope.
