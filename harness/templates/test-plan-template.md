# Test Plan Template

Record planned test cases, preconditions, steps, and expected outcomes here; record actual execution status and evidence in `summary_v<N>.md`.

## Feature / Bug
> One-line description of what is being tested.

## Rule Applicability Test Mapping
Canonical decisions live in `spec_v<N>.md#rule-applicability`. Map each `Required` rule ID below:

| Required Rule ID | Planned verification |
|---|---|
| `<RULE_ID>` | `<test IDs, Gradle task, or review check>` |

## Layer Selection

| Layer | Included | Reason |
|---|---|---|
| Unit tests (`app/src/test/`) | ✅ / ❌ | |
| Integration tests (`app/src/test/` or loopback boundary) | ✅ / ❌ | |
| Instrumented UI tests (`app/src/androidTest/`) | ✅ / ❌ | |
| Ephemeral exploration (`exploratory-qa` / `agent-device`) | ✅ / ❌ | |

## Ephemeral QA Cases *(when included; else `Not applicable — <reason>`)*
Store temporary artifacts under `.scratch/qa/<run-id>/` and remove `.scratch/` before commit.

| ID | Requirement / question | Preconditions / fixture | Numbered steps | Expected result & cleanup |
|---|---|---|---|---|
| QA-01 | `<requirement>` | `<build/device/fixture>` | `1. ... 2. ... 3. ...` | `<observable outcome + restoration>` |

## Production Journey & IME Boundary *(when `NAV: Required` or IME inputs are in scope)*
- **Journey test**: `app/src/androidTest/.../<Class>Test.kt#<method>` (entry point, real UI gestures, return boundary, post-return assertion).
- **IME test**: `app/src/androidTest/.../<ImeTest>.kt#<method>` (`createAndroidComposeRule<MainActivity>()`, focus input, verify keyboard-open bounds and dismissal).

## Maintained Test Cases

### `<ClassName>Test.kt` — Unit / Integration

| ID | Layer | Given | When | Then | Scenario / Fixture |
|---|---|---|---|---|---|
| T1 | Unit / Integration | `<precondition>` | `<action>` | `<expected outcome>` | `<scenario.json or deterministic fake>` |

#### T1 — Numbered steps
1. Arrange precondition.
2. Trigger action on production entry point.
3. Assert expected state/output.

## Coverage & Verification Commands
- Overall line coverage `>= 80%`; new ViewModels / UseCases `>= 90%`.
- Commands (from [.docs/VALIDATION.md](../../.docs/VALIDATION.md)):
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:koverXmlReportDebug`
  - `./gradlew :app:connectedDebugAndroidTest` *(when instrumented runtime boundary is in scope)*
