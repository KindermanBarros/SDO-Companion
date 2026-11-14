---
name: android-testing
description: Implement and run planned Android unit, integration, and instrumented tests.
---
# Skill — Android Testing

Follow [testing-strategy.md](../../rules/testing-strategy.md), [testing-practices.md](../../rules/testing-practices.md), and [.docs/VALIDATION.md](../../../.docs/VALIDATION.md). Ephemeral QA belongs to [exploratory-qa](../exploratory-qa/SKILL.md).

## Execution
1. **Select scope**: Identify the lowest test layer that proves the changed behavior:
   - **Unit (`app/src/test/`)**: Domain rules, use cases, catalog/progression calculations, ViewModel state transitions ([android-unit-test](../android-unit-test/SKILL.md)).
   - **Integration (`app/src/test/` or `app/src/androidTest/`)**: Repository coordination, serialization, Firestore/Room mapping ([android-integration-test](../android-integration-test/SKILL.md)).
   - **Instrumented (`app/src/androidTest/`)**: Room SQLite migrations, Compose semantics/gestures, `AppSurface` back-stack restoration, and IME bounds ([testing-runtime-evidence.md](../../rules/testing-runtime-evidence.md)).
2. **Author tests**: Use explicit sorted imports (no wildcard or inline FQNs), descriptive names, and AAA structure. Use only libraries declared in [.docs/TECH_STACK.md](../../../.docs/TECH_STACK.md) (JUnit 4, AndroidX test, Compose UI test, coroutines).
3. **Run scoped selectors once**:
   - JVM: `.\gradlew.bat :app:testDebugUnitTest --tests "<ClassName>"`
   - Android runtime: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="<ClassName>#<method>"`
4. **Record evidence**: Record exact command, exit code, non-zero executed count, and any runtime limitation in the active summary.
