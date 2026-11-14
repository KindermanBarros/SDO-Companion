# VALIDATION - SDO Companion
Select the smallest sufficient checks once before execution. The [CI catalog](../.agents/gates/ci-checks.md) owns commands; it is not a requirement to run every command for every task.

## Selection
| Changed boundary | Relevant evidence |
|---|---|
| Documentation only | Links, anchors, line limits and scoped diff; no Android build/device suite |
| Harness/gate/hook | Structural gate plus regression fixtures for the changed validator |
| Small UI/copy/layout | Build and focused review/capture; reuse existing reference states |
| Behavioral defect | One relevant RED/GREEN regression, then related checks |
| Data/domain/API/security | Relevant unit/integration/security checks and triggered lint/coverage |
| Navigation/lifecycle/IME/platform | Actual changed runtime boundary; JVM/component tests cannot substitute |

An explicit owner instruction to use CI/build-only and manually verify runtime takes precedence for that task. Record the limitation; it does not grant merge/release acceptance.

## Commands
Windows from the repository root; POSIX uses ./gradlew.
| Check | Command |
|---|---|
| JVM | .\gradlew.bat :app:testDebugUnitTest |
| Android | .\gradlew.bat :app:connectedDebugAndroidTest |
| Lint | .\gradlew.bat :app:lintDebug |
| Ktlint | .\gradlew.bat :app:ktlintCheck |
| Detekt | .\gradlew.bat :app:detekt |
| Coverage | .\gradlew.bat :app:koverXmlReportDebug |
| Build | .\gradlew.bat :app:assembleDebug |

Harness: powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/validate-harness.ps1.
Run .agents/gates/tests/validate-harness-tests.ps1 when structural policy changes, and scratch-cleanup-tests.ps1 when cleanup enforcement changes. Tests create fixtures only in .scratch and clean them in finally.

## Stop repeated work
- One verification plan, one result summary. A stage handoff does not invalidate a successful check.
- Reuse evidence only for unchanged source/config/command/runtime; identify the commit or receipt.
- Rerun the affected check after a relevant change/failure; broaden only for a concrete unresolved risk.
- Inspect concise failures/report excerpts, not entire logs. Raw logs/downloads/PR drafts stay in .scratch and are removed before commit.
- Record failed, skipped or unavailable checks honestly. Stop at an external blocker; do not repeat the same failed prerequisite.
- Use existing JUnit/AndroidX patterns and focused selectors; add no test dependency without approval.
- Coverage thresholds remain 80% overall and 90% for new ViewModels/use cases when applicable; report generation alone is not acceptance.
- UI screenshots and approved references are different artifacts. Missing runtime/pixel evidence is not PASS.
- Before commit remove .scratch entirely and run the pre-commit hook plus scoped git diff --check.

[Test inventory](Validation/test_inventory.md) is historical inventory; current test counts come from actual reports.
