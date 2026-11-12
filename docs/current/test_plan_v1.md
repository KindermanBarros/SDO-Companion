# Test Plan — SDO Companion adoption baseline

Status: proposed for approval. This file defines procedures only. Record outcomes in `summary_v2.md`.

## Feature / Bug

Verify the adoption profile and establish the existing application's baseline without modifying application or test source.

## Rule Applicability Test Mapping

Canonical decisions: [spec_v1.md#rule-applicability](spec_v1.md#rule-applicability).

| Required Rule ID | Planned verification |
|---|---|
| ARCH | OB-01 source/profile reconciliation; OB-02 canonical full-source checks; document manual DI/navigation policy gaps |
| IMPL | OB-08 diff/ownership review; no production code generated or callbacks changed |
| TEST | OB-03 build, OB-04 fresh JVM XML, OB-05 quality/coverage prerequisites, OB-06 real platform boundary, OB-07 lifecycle/product/approval checks |

## Layer Selection

Execution intent: baseline verification of existing maintained tests; no new regression suite or ephemeral exploration is commissioned.

| Layer | Included | Reason |
|---|---|---|
| Unit tests | Existing suite | Establish fresh business-rule and mapper baseline |
| Integration tests | Existing suite where present | Establish existing boundaries; do not claim missing integration coverage |
| Instrumented tests | Existing tests, subject to runtime prerequisites | Room and Compose require real Android evidence |
| Ephemeral exploration | No | No exploratory QA or API mocking requested |

## Ephemeral Test Cases

Not applicable; no device-exploration session or temporary backend fixtures are proposed.

## Production Journey Boundary

No NAV change is proposed. Existing custom navigation lacks the harness production journey contracts; mark this as an unresolved adoption gap. Component tests cannot establish shipped navigation correctness.

## IME Production Boundary

No input/IME change is proposed and no IME acceptance claim is made. Existing responsive/component tests do not prove production keyboard behavior.

## Test Cases

| ID | Requirement | Given / When / Then |
|---|---|---|
| OB-01 | FR-001 / AC-001 | Given settings/build/source evidence, when profile roots and fingerprints are checked, then :app and every declared source root resolve within the repo |
| OB-02 | FR-003 / AC-003, AC-005 | Given the full source tree, when the canonical bundle runs, then preserve its exit and complete findings; failures remain blockers |
| OB-03 | FR-003 / AC-003 | Given available approved dependencies, when :app:assembleDebug runs, then require exit 0 and the debug APK; failed resolution is not a build pass |
| OB-04 | FR-003 / AC-004 | Given the existing JVM suite, when clean/rerun tasks run, then parse fresh Gradle XML; require non-zero executed tests and zero errors, failures and skips |
| OB-05 | FR-003 / AC-005 | Given Gradle task inventory, when quality/coverage tasks are resolved, then execute existing tasks or record missing configuration; no plugin edits or suppressions |
| OB-06 | FR-003 / AC-005 | Given a deterministic Android runtime, when the existing real SQLite migration test executes, then require named method execution and passing instrumentation XML; absent runtime is blocked |
| OB-07 | FR-004 / AC-006, AC-007 | Given exact user-approved inputs, when onboarding verification runs, then require matching approval hashes, empty resolved unknowns and all required gates before successful cleanup |
| OB-08 | FR-002 / AC-002 | Given initial user changes and final diff, when ownership is reviewed, then app/tests/catalogs/instructions/memory have no onboarding edits |

### OB-01 — Profile and discovery

1. Compare settings.gradle.kts, app/build.gradle.kts and version catalog against the profile and plan.
2. Verify main/test/androidTest roots; verify root catalogs is an additional resources directory.
3. Run `python harness/scripts/project_profile.py` and `python harness/scripts/project_onboarding.py check` using the selected installed interpreter.
4. Require exit 0, then independently retain unresolved runtime and policy questions; a shape check cannot settle them.

### OB-02 — Full-source baseline

1. Set process-local HARNESS_PYTHON and PYTHONIOENCODING=utf-8.
2. Run `bash harness/scripts/check-full-source-rules.sh` with stdout/stderr in a referenced log.
3. Preserve exit status and distinguish app findings from intentionally failing negative contract fixtures.
4. Do not broaden exclusions, add baselines, suppress findings or modify source.

### OB-03 and OB-04 — Build and JVM suite

1. Configure process-local JAVA_HOME, ANDROID_HOME and PATH to existing JBR 25, SDK and Git Bash. Confirm dependencies are cached or separately approved for download.
2. Run `cmd /c gradlew.bat :app:assembleDebug`; retain exit and artifact path/hash.
3. Record execution start time. Run `cmd /c gradlew.bat :app:cleanTestDebugUnitTest :app:testDebugUnitTest --rerun-tasks`.
4. Parse `app/build/test-results/testDebugUnitTest/TEST-*.xml`, checking freshness, failures/errors/skips and executed testcase count. Missing XML or zero tests fails.

### OB-05 — Quality and coverage

1. Inspect `gradlew.bat tasks --all` without installing new plugins.
2. Run `gradlew.bat ktlintCheck`, `gradlew.bat detekt`, `gradlew.bat :app:lintDebug` and `gradlew.bat :app:koverXmlReportDebug` where available; missing required tasks remain blocked.
3. With a fresh Kover report run `bash harness/scripts/check-coverage.sh app/build/reports/kover/reportDebug.xml`; require overall coverage at least 80%. No new ViewModel/use-case classes are proposed; their new-class 90% requirement is not triggered.

### OB-06 — Existing real Android boundary

| Capability | Runtime prerequisite | Source-backed target | Required result |
|---|---|---|---|
| Room migrations | Android API >=30 device/emulator, built debug/test APKs, no production backend writes | `app/src/androidTest/java/com/kinderman/sdo/data/local/AppDatabaseMigrationInstrumentedTest.kt#migration27To28AddsRemainingCanonicalPayloadsIdempotently` | Actual SQLite migration assertions pass; non-zero executed XML |
| Compose component suite | Compatible runtime and deterministic test fixtures | Existing CyberGrungeComponentsInstrumentedTest and CharacterCreationWizardResponsiveTest | Only actual executed component claims may be reported; no production journey or visual parity inference |

1. Inspect shipped test setup/runner and prove production Firebase writes cannot occur during the selected tests. If isolation is unknown, stop runtime execution and report blocked.
2. Select an available emulator first; record serial, API, locale and build fingerprint. No emulator/dependency download is authorized by this plan.
3. Execute the named migration test using `ANDROID_SERIAL=<device> bash harness/scripts/run-android-instrumented-tests.sh --class com.kinderman.sdo.data.local.AppDatabaseMigrationInstrumentedTest#migration27To28AddsRemainingCanonicalPayloadsIdempotently` only after runner compatibility is established.
4. Require executed method XML and matching runtime/build evidence. Do not replace this with JVM fakes or skipped tests.

### OB-07 — Documentation, approval and completion

1. Run `bash harness/scripts/check-feature-lifecycle.sh --allow-empty` for the initial tracker.
2. Use a supported Python interpreter to run `harness/scripts/check-product-docs.py --change-dir docs/current --summary docs/current/summary_v2.md`. Python without pathlib.is_relative_to support is a prerequisite failure, not permission to weaken the check.
3. Record actual approval only after consent, with exact plan/profile hashes and linked artifact bindings.
4. Once all unknowns are resolved, refresh discovery and run onboarding `check` then `verify`. Known failures prevent completion; record them without forced cleanup.
5. Review successful cleanup receipt against the installation manifest; preserve user edits and persistent setup evidence.

### OB-08 — Preservation

1. Compare final git status/diffs against the initial recorded user changes.
2. Confirm modifications belong only to the approved profile/documents/evidence list.
3. Confirm no app, test, catalog, protected instruction or existing memory file was edited by onboarding.

## Shared JSON Scenarios

Not applicable; no backend endpoint change or new API integration test is proposed. Existing SDK data tests do not become proof of live Firebase transport.

## Visual Comparison

Not applicable; no visual change or visual-parity claim. No screenshots or implementation-recorded goldens are acceptance substitutes.

## Coverage Targets

Overall >=80% when a compatible report is available; missing Kover configuration remains a gate blocker. No new production classes are planned.

## Verification Commands

Exact commands and prerequisites are declared per case above and in implementation_plan_v2.md. Downloads, task/plugin repairs, app/test edits and migration exceptions require a separate approved revision.
