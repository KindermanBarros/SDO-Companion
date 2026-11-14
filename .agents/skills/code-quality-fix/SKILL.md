---
name: code-quality-fix
description: Run static analysis, formatting, lint checks, and fix violations.
---

# Skill — Code Quality Fix

## Execute
1. Run applicable quality checks from [.docs/VALIDATION.md](../../../.docs/VALIDATION.md):
   - `./gradlew :app:assembleDebug`
   - `./gradlew :app:ktlintCheck` (or `./gradlew :app:ktlintFormat` to auto-fix formatting)
   - `./gradlew :app:detekt`
   - `./gradlew :app:lintDebug`
   - `./gradlew :app:koverXmlReportDebug`
2. Verify zero dummy/stub markers (`TODO()`, `NotImplementedError`, `// dummy`, `// placeholder`, `// stub`) in `app/src/main/`.
3. Fix root causes without suppressing rules or refactoring unrelated code.

## Done When
- Selected quality and coverage checks pass (or pre-existing baseline debt is explicitly separated from touched files) and recorded in the task summary.
