# PR #140 integration correction

Date: 2026-11-12. User-authorized validation: GitHub CI only; local test and harness execution gates are explicitly waived for this correction. No runtime or local build success is claimed.

## Scope

- Rebase onto origin/main was a no-op: branch already up to date at dd6f32a5cf35cae5ee5b14b78e4e282b421cdaa0.
- Both verify and release jobs now execute the repository Gradle wrapper; setup-gradle only manages cache/setup and no longer installs an independently pinned Gradle 9.7.1. This exercises the proposed Gradle 9.9.0 distribution in CI.
- KSP remains at 2.3.12, deferring the 2.3.13 update because its upstream release documents a concurrent in-process execution regression. That regression has not been reproduced in this app.
- App behavior, authority, persisted schemas and catalog requirements remain unchanged. No Firebase secrets or authentication configuration changes.

## Verification and follow-up

Source-level workflow inspection verifies both Gradle invocations use ./gradlew and neither setup-gradle step supplies gradle-version. Local Android checks are not run under the user’s CI-only instruction. The prior environment bootstrap could not download Gradle (Network is unreachable) and exposed Java 21 rather than the required Java 25. These are not application test failures.

Required remaining evidence: the updated PR’s GitHub Actions verify job, including catalog validation, lintDebug, testDebugUnitTest and assembleDebugAndroidTest, must pass. Release packaging remains exercised on trusted main under the existing workflow. Independent evaluator acceptance is pending CI results; this record does not claim full harness acceptance or authorize merging a failing check.
