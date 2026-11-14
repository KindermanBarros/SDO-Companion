# CI checks
Select once using [VALIDATION](../../.docs/VALIDATION.md); stage transitions do not require rerunning unchanged checks.

## Harness/documentation
- Structural: powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/validate-harness.ps1
- Structural regression (when changed): .agents/gates/tests/validate-harness-tests.ps1
- Cleanup regression (when changed): .agents/gates/tests/scratch-cleanup-tests.ps1
- Adaptive runner regression only when that runner changes: .agents/gates/tests/adaptive-ui-runner-tests.ps1
- Scoped staged diff: git diff --cached --check
- Commit guard: .githooks/pre-commit; .scratch must not exist, even empty.

These prove instruction/cleanup integrity, not Android behavior. Fixtures and raw logs go in .scratch and are deleted before commit.

## Application command catalog
Run only triggered checks or the explicitly requested owner/CI lane.
| Boundary | Command |
|---|---|
| Build | .\gradlew.bat :app:assembleDebug |
| JVM | .\gradlew.bat :app:testDebugUnitTest |
| Android runtime | .\gradlew.bat :app:connectedDebugAndroidTest |
| Lint | .\gradlew.bat :app:lintDebug |
| Formatting/static | .\gradlew.bat :app:ktlintCheck / :app:detekt |
| Coverage | .\gradlew.bat :app:koverXmlReportDebug |

Use class/method selectors for relevant regressions. Full suites are justified by broad changes or concrete unresolved risk, not every UI/copy edit.
Coverage thresholds remain 80% overall and 90% for new ViewModels/use cases when applicable.
No Roborazzi task/dependency is currently declared; required pixel comparison is unproven until approved infrastructure exists.
Retired script gates must not be reintroduced by generic skills.

## Evidence
One concise summary records commit, command, exit/count and relevant runtime or CI URL.
Reuse only matching source/config/command/runtime evidence. Failed, missing, skipped and historical results are not current passes.
Promote minimal final evidence/decisions into durable docs; raw logs/build archives/PR drafts stay temporary.
Stop at unresolved external blockers; do not repeat the same command hoping for different prerequisites.
