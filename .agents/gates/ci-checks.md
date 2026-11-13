# CI checks

Read [Project contract](project-contract.md). Select gates by the actual changed boundary; never report unavailable proof as PASS.

## Documentation-only harness gate

Run from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/validate-harness.ps1
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/tests/validate-harness-tests.ps1
```

This checks active harness links, line limits, imports, sealed banners and setup cleanup.
Its regression fixtures reject broken links, anchors, placeholders, oversized documents and stale setup inputs.
It does not execute Android/source-rule checks or prove application readiness.
Run scoped git diff --check for maintained changes; pre-existing unrelated whitespace is recorded separately.
When changing the adaptive CI runner, also run powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/tests/adaptive-ui-runner-tests.ps1.

## Application gates

Windows commands below use the existing :app module. POSIX uses ./gradlew instead of .\gradlew.bat.

| Gate | Command | Required evidence |
|---|---|---|
| Debug build | .\gradlew.bat :app:assembleDebug | Actual successful build |
| JVM tests | .\gradlew.bat :app:testDebugUnitTest | Fresh XML, nonzero counts, no failures |
| Ktlint | .\gradlew.bat :app:ktlintCheck | Successful task; no suppressions |
| Detekt | .\gradlew.bat :app:detekt | Successful task; no failure masking |
| Android Lint | .\gradlew.bat :app:lintDebug | No errors; review changed-file warnings |
| Coverage | .\gradlew.bat :app:koverXmlReportDebug | Report with reviewed coverage counts |
| Android boundary | .\gradlew.bat :app:connectedDebugAndroidTest | Declared runtime/build, named successful cases |

Coverage thresholds remain 80% overall and 90% for new ViewModels/domain use cases.
Review weighted XML counts and applicable per-file thresholds; report generation alone does not prove the thresholds. No separate script gate applies.
No Roborazzi task/dependency is declared in the observed baseline; a required visual comparison remains blocked until its infrastructure is explicitly planned and configured.
Use the configured Gradle instrumented task or the documented direct adb command for the declared boundary; record actual test counts and runtime identity.

## Available gates

The legacy script-backed source, stage-artifact, lifecycle, traceability, receipt, journey, platform, visual-contract and harness self-test gates have been removed.
Workflows still require meaningful tests, reviewed acceptance mappings, actual runtime evidence and human approvals; these are not claims of automated validation.
Do not require an absent checker or restore retired tooling as a prerequisite to ordinary work.
## Evidence

Every selected gate records command, exit status, executed test count where relevant, runtime identity and retained report paths.
Reuse evidence only with matching source/config/command/runtime fingerprints.
Conditional N/A needs a scope-specific reason; missing required evidence is Blocked.
Earlier onboarding reports remain historical and do not certify the current checkout.
