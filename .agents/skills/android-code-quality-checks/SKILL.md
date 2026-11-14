---
name: android-code-quality-checks
description: Run Ktlint, Detekt, Android Lint, and coverage checks from the CI catalog.
---
# Android Code Quality Checks

Select and run only the checks triggered by the changed boundary using [.docs/VALIDATION.md](../../../.docs/VALIDATION.md) and [CI checks](../../gates/ci-checks.md).

## Commands (Windows from repo root)
- **Ktlint**: `.\gradlew.bat :app:ktlintCheck` (auto-format scoped files with `.\gradlew.bat :app:ktlintFormat`)
- **Detekt**: `.\gradlew.bat :app:detekt`
- **Android Lint**: `.\gradlew.bat :app:lintDebug`
- **Coverage**: `.\gradlew.bat :app:koverXmlReportDebug` (80% overall, 90% new ViewModels/use cases when applicable)

## Rules
- Fix root causes in touched files rather than adding `@Suppress` or `tools:ignore` (unless a documented false positive is explicitly approved).
- Reuse valid fingerprint-matched results across stage handoffs; do not rerun full suites for unchanged source.
- Report pre-existing baseline violations or missing runtimes honestly as limitations, never as passing gates.
