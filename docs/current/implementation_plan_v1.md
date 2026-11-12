# SDO Companion onboarding — implementation plan v1

Date: 2026-11-12. Status: Draft; not approved and not executable yet.

## Feature / Bug

Adopt the existing SDO Companion Android application into the harness.

## Requirement Summary

Preserve the existing character-sheet, campaign, and codex implementation while establishing accurate project configuration and honest baseline evidence. This is onboarding, with no proposed product behavior or visual changes. Existing source presence does not establish feature completeness.

## Rule Applicability Implementation

The canonical specification will be `docs/current/spec_v1.md#rule-applicability`, after confirmation of adoption scope. It must contain all ten decisions before this plan can be approved. Proposed triggers: ARCH, IMPL, TEST apply; SEC applies if build/security configuration changes; UI, localization, navigation, API, observability, and analytics changes are outside this adoption proposal. Repository-wide checks still scan existing code.

Specification and test-plan approval-time SHA-256 bindings: pending their requirements-stage completion. This draft cannot authorize implementation until those files and exact hashes are bound here.

## Impact Summary

| Area | Work |
|---|---|
| Application and tests | Preserve existing files; inventory policy gaps before proposing repairs |
| Profile | Map :app, presentation/UI, domain, data, commands, and unresolved navigation contracts |
| Product documentation | Propose capability index and living requirements from existing contracts; qualify unverified behavior |
| Evidence | Record discovery, source checks, build/test/runtime availability and blockers |

## API Changes

None. Firebase Auth and Firestore are existing SDK integrations, confirmed by imports in `data/auth/FirebaseAuthRepository.kt` and `data/repository/SyncedCharacterRepository.kt`. The scanner's missing networking signal does not mean no backend. No OpenAPI endpoint change is proposed.

## Files to Create

| File | Purpose |
|---|---|
| `docs/current/spec_v1.md` | Confirmed adoption requirements and complete ten-rule matrix |
| `docs/current/test_plan_v1.md` | Baseline cases, commands and acceptance boundaries |
| `docs/product/product.md` | Capability index and initial lifecycle tracker |
| `docs/product/requirements/character-sheets.md` | Sheet capability requirements, with verification qualifications |
| `docs/product/requirements/campaigns.md` | Campaign capability requirements |
| `docs/product/requirements/codex.md` | Codex capability requirements |
| `docs/onboarding/approval.json` | Only after actual consent; exact approved input hashes |
| `docs/current/onboarding-summary.md` | Command results, evidence excerpts and remaining blockers |

Discovery already created `docs/architecture/architecture-map.{md,json}`, `harness-project.json`, and installation state. The onboarding script owns generated map refreshes and cleanup receipts.

## Files to Modify

| File | Proposed change |
|---|---|
| `harness-project.json` | Replace discovery draft commands and layer roots with confirmed mapping; retain unresolved items |
| This plan | Bind completed spec/test plan and hashes before approval |

Protected instruction file edit list: **none**. No edits to AGENTS.md, rules, workflows, or skills are proposed. Build/source/test repairs require a further exact file list and revised plan; this draft grants no blanket migration authorization.

## Files to Delete

None. Successful verification may remove only unchanged installer-owned temporary files through the installation manifest. Retain edited setup prompt, harness runtime, approvals, architecture and evidence. Never delete user changes or catalog generators/output.

## UiState Design

No new state or UI change. Existing `presentation/AppViewModel.kt`, `AuthViewModel.kt`, and Compose screens are adopted as observed; their multiple flows are not evidence of compliance with the single-UiState policy.

## Test Plan

Planned baseline sequence after requirements and plan approval:

1. Validate profile paths and architecture fingerprints with `project_onboarding.py check`.
2. Run canonical `bash harness/scripts/check-full-source-rules.sh`; preserve complete output and exit status. Set `HARNESS_PYTHON` to the installed interpreter and `PYTHONIOENCODING=utf-8` on Windows.
3. Run `cmd /c gradlew.bat :app:assembleDebug`.
4. Run `cmd /c gradlew.bat :app:cleanTestDebugUnitTest :app:testDebugUnitTest --rerun-tasks`; require fresh non-zero passing XML in `app/build/test-results/testDebugUnitTest/`.
5. Run ktlint, Detekt and Kover/coverage gates once their missing plugin configuration has an approved resolution. Commands: `gradlew.bat ktlintCheck`, `gradlew.bat detekt`, `gradlew.bat :app:koverXmlReportDebug`, and `bash harness/scripts/check-coverage.sh app/build/reports/kover/reportDebug.xml`. Missing tasks are blockers, never passes.
6. Existing Room migration and Compose instrumented tests require a real Android runtime. No device is currently connected. Use `ANDROID_SERIAL=<device> bash harness/scripts/run-android-instrumented-tests.sh --all` only after confirming runner compatibility and deterministic backend isolation. No production Firebase writes for verification.
7. Refresh discovery, reconcile all unknowns, run lifecycle and product documentation checks, then onboarding `check` and `verify`. Base verification alone does not satisfy quality, coverage or runtime gates.

## Explicit Assumptions

Proposed scope for user confirmation: adopt and document existing architecture first; do not migrate UI, DI, navigation, schemas or Firebase behavior as an incidental setup change. No approvals or policy exceptions have been recorded.

## Risks

- Manual DI: `SdoApplication.kt` constructs repositories and Room; `ViewModelFactories.kt` constructs ViewModels. Hilt policy requires a deliberate migration or explicit scoped exception.
- Custom navigation: `SdoApp.kt` owns AppSurface, selectedId and a saved string back stack. Navigation Compose is a dependency but no NavHost is used here. Missing navigation regression/contract files must remain unresolved.
- Existing UI mixes presentation screens and UI component packages. Existing strings, imports and styling may fail full-source checks; do not suppress or exclude them to obtain green evidence.
- No ktlint, Detekt or Kover plugin declarations were found in root/app builds. Adding them needs an exact build-file change proposal with compatible pinned versions.
- `docs/product/design_system.md` is absent. Existing references are `DESIGN_GUIDE.md`, `docs/interface-cybergrunge.md`, and `docs/cybergrunge-implementation-plan.md`. Adoption does not approve a replacement design system.
- Toolchain versions are declared, not resolved: Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.20, KSP 2.3.12, Compose BOM 2026.09.00. Dependency availability/compatibility remains unverified.

## Migration / Compatibility Notes

Single module `:app` at `app/`; no included builds or projectDir remaps in settings. Main/test/instrumented roots are `app/src/main/java`, `app/src/test/java`, `app/src/androidTest/java`. Resources/manifest remain under `app/src/main`; custom JVM resources include root `catalogs/`. Application ID and namespace are `com.kinderman.sdo`; min SDK 30, compile/target 37, Java 25. Room migrations through 27→28 are registered in SdoApplication; do not alter schemas during setup.

Installed tools discovered: Python at `C:/Users/kindb/Python/python.exe`, Git Bash at `C:/Program Files/Git/bin/bash.exe`, Android Studio JBR 25.0.4, Android SDK platform `android-37.0`, and adb. Default PATH lacks usable Python/Java/Bash entries. Prefer process-local environment configuration; do not silently change global PATH. No dependency installation is authorized by this draft. Propose exact missing downloads after baseline resolution; do not install browsers for this non-UI scope.

## Out of Scope

New product features, visual redesign, catalog regeneration, release signing changes, Firebase deployment, production data writes, publishing, and edits to existing memory or instruction files.

## Approval prerequisites

Confirm adoption scope, complete and approve canonical spec/test plan, bind their hashes, resolve architectural policy decisions, and enumerate any required repairs before implementation approval. Draft profile unknowns intentionally remain populated. Full access is filesystem permission, not recorded implementation approval.
