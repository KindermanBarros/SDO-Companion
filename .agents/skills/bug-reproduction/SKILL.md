---
name: bug-reproduction
description: Reproduce a behavioral bug with a failing test before fixing it.
---
# Skill — Bug Reproduction (TDD)

Follow [Bug Fixing](../../workflows/bug-fixing.md) and [testing-strategy.md](../../rules/testing-strategy.md). Apply to behavioral defects; cosmetic/copy/documentation adjustments use proportional build/review verification without ceremonial RED tests.

## Execution
1. **Select lowest reproduction layer**:
   - Domain/calculation/ViewModel state bug → JVM unit test (`app/src/test/`)
   - Repository/serialization/sync mapping bug → JVM integration test (`app/src/test/`)
   - Room migration, Compose gesture/semantics, or `AppSurface` lifecycle bug → Instrumented test (`app/src/androidTest/`)
2. **Write minimal RED test**: Do not modify production code yet. Name the test clearly (`given_when_then`) and target the exact root cause. If needed from an `exploratory-qa` handoff, translate its recorded finding into this deterministic test without re-running the QA session.
3. **Confirm RED**: Run the single test selector (`.\gradlew.bat :app:testDebugUnitTest --tests "<ClassName>"` or `connectedDebugAndroidTest` with `-Pandroid.testInstrumentationRunnerArguments.class="<ClassName>#<method>"`) and verify it fails for the expected reason before implementing the fix.
