# Testing Strategy Rules

Select the smallest sufficient checks once using [VALIDATION](../../.docs/VALIDATION.md) and [CI checks](../gates/ci-checks.md).

## Execution intent
- Ephemeral device/backend exploration uses `exploratory-qa` with `agent-device`; keep scenarios, captures and cleanup receipts in `.scratch/qa/<run-id>/` and delete `.scratch` before commit. Do not add permanent `androidTest` classes or CI jobs for temporary QA runs.
- Maintained application changes use the repository test pyramid: JVM unit tests (`app/src/test/`) first, JVM/loopback integration tests for repository/serialization/sync contracts, and `app/src/androidTest/` only for real Compose rendering, gestures, navigation, Room migration, IME or Android SDK boundaries.

## Layer & coverage policy
- Unit/integration tests end with `Test.kt` or `IntegrationTest.kt` and keep one primary scenario per test.
- Bug fixes require a failing RED regression test before the fix and GREEN verification after; cosmetic/documentation edits use proportional build/review checks.
- Coverage targets (verified via `.\gradlew.bat :app:koverXmlReportDebug` when applicable): 80% overall, 90% for new ViewModels and domain use cases. Compose UI screens are excluded from line coverage and verified through runtime semantics or captures when triggered.
- Keep contract payloads in shared/resource fixtures rather than large inline JSON strings.

## Evidence Invariants
- A check passes only when the command ran, exited 0, executed a non-zero test count, and produced source-fed evidence.
- Missing runtimes, devices, skipped tests, fake-only platform proofs, or unexecuted commands are never reported as passing.
- Reuse prior results only when source, configuration, command, and runtime fingerprints match; record command, exit code, counts, and limitations once in the active summary.
