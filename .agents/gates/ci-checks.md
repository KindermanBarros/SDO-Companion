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

## Application gates

Windows commands below use the existing :app module. POSIX uses ./gradlew instead of .\gradlew.bat.

| Gate | Command | Required evidence |
|---|---|---|
| Debug build | .\gradlew.bat :app:assembleDebug | Actual successful build |
| JVM tests | .\gradlew.bat :app:testDebugUnitTest | Fresh XML, nonzero counts, no failures |
| Ktlint | .\gradlew.bat :app:ktlintCheck | Successful task; no suppressions |
| Detekt | .\gradlew.bat :app:detekt | Successful task; no failure masking |
| Android Lint | .\gradlew.bat :app:lintDebug | No errors; review changed-file warnings |
| Coverage | .\gradlew.bat :app:koverXmlReportDebug | Report and mandatory threshold validation |
| Android boundary | .\gradlew.bat :app:connectedDebugAndroidTest | Declared runtime/build, named successful cases |
| Source rules | bash harness/scripts/check-full-source-rules.sh | Complete production/test scan; zero exit |

Coverage thresholds remain 80% overall and 90% for new ViewModels/domain use cases.
Validate weighted XML counts and per-file thresholds with the required checker; report generation alone does not prove the thresholds.
No Roborazzi task/dependency is declared in the observed baseline; a required visual comparison remains blocked until its infrastructure is explicitly planned and configured.
Do not use connectedDebugAndroidTest as a substitute for the direct instrumented runner when the approved contract requires installation-preservation receipts.

## Detailed contracts

[Required source and conditional contracts](Contracts/source_and_runtime.md) preserves the command inventory and trigger requirements.
Its full-source, stage, lifecycle, acceptance, runtime and visual validators remain mandatory for their declared claims.
The harness/scripts and harness/templates paths are currently absent; record missing prerequisites and stop affected application stages.
Do not restore deleted tooling or modify product code merely to make documentation checks green.

## Evidence

Every selected gate records command, exit status, executed test count where relevant, runtime identity and retained report paths.
Reuse evidence only with matching source/config/command/runtime fingerprints.
Conditional N/A needs a scope-specific reason; missing required evidence is Blocked.
Earlier onboarding reports remain historical and do not certify the current checkout.
