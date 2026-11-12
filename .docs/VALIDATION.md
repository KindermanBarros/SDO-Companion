# VALIDATION — SDO Companion

Dependencies: [TECH_STACK](TECH_STACK.md#dependencies).

## Test types

| Type | Source set | Runtime | Observed scope |
|---|---|---|---|
| Local | app/src/test/java/ | JVM, JUnit 4.13.2 | 41 Kotlin files, including one migration-support file |
| Instrumented | app/src/androidTest/java/ | AndroidJUnitRunner | 5 Kotlin test files; actual Compose UI tests exist |

[Test inventory](Validation/test_inventory.md) names every current test class and summarizes its source assertions.
No dedicated screenshot suite was found. ui-test-manifest is a debug-only dependency; UI-test existence is established by the androidTest sources.

## Test creation pattern

- Mirror production packages and use existing descriptive test names.
- Arrange, act, assert; verify observable behavior using existing domain rules and repository boundaries.
- JUnit 4 and AndroidX JUnit/Compose testing are declared.
- No MockK, Mockito, Turbine or kotlinx-coroutines-test dependency is declared; do not assume runTest is available.
- Current source tests do not establish complete ViewModel, Firebase or end-to-end coverage.

## Checks

Commands run from the root on Windows; POSIX uses ./gradlew instead of .\gradlew.bat.

| Check | Command |
|---|---|
| Unit tests | .\gradlew.bat :app:testDebugUnitTest |
| Instrumented | .\gradlew.bat :app:connectedDebugAndroidTest |
| Android Lint | .\gradlew.bat :app:lintDebug |
| Ktlint | .\gradlew.bat :app:ktlintCheck |
| Detekt | .\gradlew.bat :app:detekt |
| Coverage report | .\gradlew.bat :app:koverXmlReportDebug |
| Full build | .\gradlew.bat build |

Existing source-rule scripts and project_onboarding.py are deleted in the initial working tree; do not claim their checks pass.
Prior build, test and gate results in docs/current/onboarding-summary.md are historical evidence, not current-run results.

For documentation-only changes, check all placeholders, line limits, relative file links and anchors, CLAUDE import and allowed file scope. Build/device tests are not required and cannot establish documentation correctness.

For harness-only changes run powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/validate-harness.ps1 and tests/validate-harness-tests.ps1.
The [CI checks](../.agents/gates/ci-checks.md) distinguish structural validation from required application gates.

## Device workflow validation

None for dedicated camera, Bluetooth or sensor hardware. Android UI, authentication, lifecycle transitions and network failure behavior still need runtime evidence for application changes; confirmed target is smartphone, specific models Pending.

## UI tasks and screenshots

Capture screenshots with Android CLI or adb for UI changes. This setup changes documentation only and requires no UI screenshots.
