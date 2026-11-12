# SDO Companion onboarding handoff

Date: 2026-11-12. Mode: adopt. **Harness tooling is operational; full onboarding acceptance is blocked.** No complete-app or green-onboarding claim is made.

## Authorization and scope

The owner approved plan v2 with “Sure”, then explicitly preapproved remaining harness setup changes and stage transitions. `docs/onboarding/setup-authorization.json` preserves that statement. `docs/onboarding/approval.json` binds the current plan/profile/spec/test/capability artifacts under that prospective authorization, not a claim that the user individually reviewed later revisions.

Canonical scope/applicability: `docs/current/spec_v2.md#rule-applicability`. Implementation/test contracts: `implementation_plan_v3.md` and `test_plan_v2.md`. The specification/plan skills were followed by reading repository SKILL.md files because no Skill invocation interface is exposed.

## Adopted configuration

Existing :app at app/, package com.kinderman.sdo, Android API 30 minimum / 37 compile and target, JBR 25, Kotlin/Compose/Room/Firebase. The source map covers main, test and androidTest roots; presentation and shared UI are both mapped. Root catalogs/ is an additional JVM resources input. No application logic, schemas, catalogs or UI behavior changed.

Added Ktlint 14.2.0, Detekt 2.0.0-alpha.6 and Kover 0.9.11 with enforced defaults; no suppressions, debt baselines, ignoreFailures or broader coverage exclusions. Added explicit application/test/runner identifiers, Windows environment/runtime setup and canonical native launcher delegation. Fixed UTF-8 documentation decoding and fixture-root isolation. Existing AGENTS.md, README.md and memory changes were preserved.

## Verification

Run directory: `.harness-state/onboarding-baseline-v2/` (local ignored evidence; persistent reports summarized here). Passing command evidence and limitations are separate from full onboarding acceptance.

| Check | Actual result | Evidence / excerpt |
|---|---|---|
| Discovery / profile freshness | Exit 0 | `architecture-map.md` and `fingerprint_check.log`: `PASS: project profile and architecture fingerprints` |
| Initial lifecycle tracker | Exit 0; empty tracker explicitly allowed for onboarding | `lifecycle.log`: `Feature lifecycle tracker valid: 0 feature(s), 0 in progress.` |
| Product documentation | Exit 0 | Actual product validator: `PASS: 3 capability documents; product/scratch paths and references valid.` |
| Documentation contracts | Exit 0; 15 tests | `product-contract-green.log`: `Ran 15 tests` / `OK` |
| Onboarding contracts | Exit 0; 35 tests | `onboarding-contract-green.log`: `Ran 35 tests` / `OK` |
| AST fixture isolation | Exit 0 | `ast-contract-green.log`: `PASS: Kotlin AST checker ignores rule-shaped comments/literals and rejects actual violations.` |
| Profile-aware runner contracts | Exit 0 | `runner-profile-green.log`: `honors app profiles and rejects failed/empty runs` |
| Debug build / instrumented APK build | Exit 0 | `instrumented.log`: `BUILD SUCCESSFUL`; both APKs produced from the configured build |
| JVM suite | Exit 0; 241 tests, 0 failures/errors/skips in 39 fresh XML reports | `unit-online.log`, `unit-counts.json`, `app/build/test-results/testDebugUnitTest/` |
| Real Room migration | Exit 0; one named test, API 37 emulator-5554 | `instrumented/TEST-com.kinderman.sdo.data.local.AppDatabaseMigrationInstrumentedTest_migration27To28AddsRemainingCanonicalPayloadsIdempotently.xml`: `tests="1" failures="0"` |
| Android Lint | Task succeeded; 0 errors, 12 warnings, 2 hints | `app/build/reports/lint-results-debug.txt`: `0 errors, 12 warnings, 2 hints` |
| Full-source rules | Exit 1 | `native-source-final.log`: `FAIL: full-source rules bundle (one or more checkers failed)` |
| Ktlint / Detekt | Tasks failed against actual source; combined quality command exit 1 | `quality-runtime-build.log`, `app/build/reports/ktlint/`, `app/build/reports/detekt/` |
| Coverage | Fresh report generated; threshold command exit 1 | `coverage.log`: `Overall project-owned line coverage: 61.24% (25407/41490)`; mandatory minimum stays 80% |
| Final onboarding verifier | Exit 1; completion and cleanup refused | `final-verifier.log`: `FAIL: resolve profile unknowns before verification` |

The combined quality/build command used --continue so successful Lint/report/APK tasks could be measured alongside failing style/analysis tasks; its overall exit is still 1. The initial offline build/unit/task-inventory attempts failed because dependencies were uncached. Later authorized downloads enabled the actual build/test passes above.

## Real runtime boundary

The existing Medium_Phone_API_37.0 AVD booted without a data wipe. Runtime fingerprint: `google/sdk_gphone64_x86_64/emu64xa:17/CE2A.260420.051.A2/16418371:user/release-keys`. The direct runner selected `com.kinderman.sdo.test/androidx.test.runner.AndroidJUnitRunner`, executed the declared real SQLite migration method and confirmed both packages remained installed. This proves that boundary only; it does not certify all UI, navigation, authentication or Firebase behavior. The emulator started by this setup is shut down after evidence collection; app data is not wiped.

## Remaining adoption blockers

The source scan reports 777 findings: 84 architecture, 227 Compose, 463 localization and 3 navigation. Ktlint and Detekt reveal further existing style/complexity debt. Coverage is below policy. Manual DI, the Android dependency in the domain auth interface, and custom AppSurface navigation require deliberate application work or explicitly documented permitted policy decisions. No architecture exception or gate bypass has been invented. The profile retains these unresolved items.

## Cleanup and next delivery step

No installer cleanup is eligible because required acceptance gates failed. Preserve setup_prompt.md, runtime harness, edited user files, approval history, architecture and evidence. Use `powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File harness/scripts/setup-windows.ps1` to validate the installed local runtime. Environment setup is process-local through `harness/scripts/windows-env.ps1`; native source checks use `harness/scripts/check-full-source-rules.cmd`.

The next product-delivery step is resolving the recorded application adoption debt while preserving RPG behavior, then rerunning full onboarding verification before selecting another feature. [Draft PR #141](https://github.com/KindermanBarros/SDO-Companion/pull/141) captures the harness setup and compatibility repairs; no merge, release, Firebase deployment or production-data operation is performed.
