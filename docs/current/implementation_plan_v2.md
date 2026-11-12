# SDO Companion onboarding — implementation plan v2

Date: 2026-11-12. Status: awaiting explicit approval of the complete baseline package. Supersedes the incomplete v1 draft.

## Feature / Bug

Adopt and establish a reproducible baseline for the existing SDO Companion app.

## Requirement Summary

The user confirmed adoption of the existing architecture, documentation of gaps, and separate proposals for repairs. This plan authorizes local baseline verification and documentation reconciliation only. It does not approve policy exceptions or application changes and cannot yield successful onboarding while current required gates fail.

## Rule Applicability Implementation

Canonical decisions: [spec_v1.md#rule-applicability](spec_v1.md#rule-applicability). The specification and test plan are submitted together under the onboarding workflow's joint approval mechanism.

| Required rule | Work and evidence |
|---|---|
| ARCH | Profile/source reconciliation, observed map and OB-01/OB-02; preserve DI/navigation gaps |
| IMPL | No application source authored; OB-08 proves diff ownership and preservation |
| TEST | Execute the existing tests/checks under test_plan_v1.md; require fresh non-zero XML for any pass and document unavailable gates |

## Impact Summary

| Layer | Files affected | Change type |
|---|---|---|
| UI / Presentation / Domain / Data / Navigation | None | Existing source preserved |
| Tests | None | Existing suite execution only |
| Profile | harness-project.json | Draft source/layer/command mapping |
| Documentation | Exact list below | New onboarding and product records |

## API Changes

Classification: none. No fields, DTOs, endpoints, schemas or error contracts change. No force update. Existing Firebase SDK services are recorded; no OpenAPI change is required.

## Files to Create

The following new drafts are already prepared for review; approval authorizes their adoption as the baseline documentation:

- `docs/current/spec_v1.md`
- `docs/current/test_plan_v1.md`
- `docs/current/implementation_plan_v2.md`
- `docs/current/summary_v2.md`
- `docs/product/product.md`
- `docs/product/design_system.md` (registry of existing design references only)
- `docs/product/requirements/README.md`
- `docs/product/requirements/character-sheets.md`
- `docs/product/requirements/campaigns.md`
- `docs/product/requirements/codex.md`

After actual approval create `docs/onboarding/approval.json` with actual approver/time, this exact plan path/hash and exact profile hash. Subsequent baseline evidence goes to `.harness-state/onboarding-baseline-v2/`; final handoff goes to `docs/current/onboarding-summary.md`. No other file creation or instruction edits are authorized by this list beyond installer-owned state/logs/reports from the named commands.

## Files to Modify

- `harness-project.json`: current draft maps :app main/test/androidTest roots and both ui/presentation into the UI boundary. Keep unresolved policy/runtime unknowns populated until evidenced resolution; any profile revision requires renewed hash approval.
- `docs/architecture/architecture-map.md` and `.json`: only through installer `discover --refresh` with ownership checks.
- `docs/current/summary_v2.md` and `docs/current/onboarding-summary.md`: actual stage/verification results and reconciliation.
- `docs/onboarding/discovery-review.md`: record verified follow-up discoveries and the Gradle bootstrap incident.

Protected instruction file edit list: **none**. Existing source, tests, build files, catalog output, setup prompt, README, AGENTS.md and memory are outside modification scope. v1 remains a historical draft.

## Files to Delete

None. Installer cleanup may run only after successful required verification and may remove only unchanged manifest-owned temporary files. Shared Gradle/tool caches are retained; do not uninstall downloaded tools as cleanup.

## UiState Design

No new UiState, screen, navigation or input state. No UX/Pen stage is triggered. Existing per-flow state remains an observed policy gap, without an approved exception.

## Test Plan

See [test_plan_v1.md](test_plan_v1.md), OB-01 through OB-08. Establish process-local tool paths and UTF-8 output; do not alter global PATH. Run cached/offline diagnostics first. New dependency/tool downloads require a separate approval specifying the package/version/source; approval of this plan alone does not authorize installation.

Approved build command: `["cmd", "/c", "gradlew.bat", ":app:assembleDebug"]`.

Approved unit command: `["cmd", "/c", "gradlew.bat", ":app:cleanTestDebugUnitTest", ":app:testDebugUnitTest", "--rerun-tasks"]`.

Canonical source command: `["bash", "harness/scripts/check-full-source-rules.sh"]`.

Use offline mode for initial dependency diagnostics until download approval is granted; the profile's eventual verifier commands require cached dependencies or explicit download consent. Record any cache miss without silently switching to network resolution. Gradle wrapper 9.7.1 is now cached after the separately documented bootstrap incident.

Quality commands: `gradlew.bat ktlintCheck`, `gradlew.bat detekt`, `gradlew.bat :app:lintDebug`, `gradlew.bat :app:koverXmlReportDebug`; coverage gate: `bash harness/scripts/check-coverage.sh app/build/reports/kover/reportDebug.xml`. Missing task plugins are blockers; no build edits are allowed.

Run lifecycle validation with --allow-empty for the initial tracker, source/profile checks and the product-doc check where runtime prerequisites permit. Full onboarding `verify` must remain blocked until all profile unknowns and policy/required gates are resolved. The test plan declares the existing real Android boundary and precise method; no missing device is counted as passing.

## Explicit Assumptions

No new product assumptions. Source evidence is not acceptance or architectural approval. User “Ok” confirmed the adoption scope only; no plan approval is recorded before the full package is presented.

## Risks

1. Existing Compose/localization failures and custom-navigation contract failures prevent green onboarding. Preserve complete findings and propose exact repairs separately.
2. Manual DI and multiple presentation flows differ from harness policy. This baseline grants no exception; do not silently migrate or weaken checks.
3. Ktlint, Detekt and Kover plugin declarations are absent in inspected root/app builds. Confirm task inventory and retain blockers.
4. Python 3.8.5 can run discovery but lacks pathlib.is_relative_to used by product checks. A supported interpreter must be found or a separately approved installation proposed. No checker shim/patch is allowed to fabricate a pass.
5. No connected Android runtime. Do not claim real Room, authentication, navigation, UI or lifecycle evidence from JVM tests. Inspect selected instrumented tests for production Firebase writes before execution.
6. A Gradle --offline --version diagnostic bootstrapped Gradle 9.7.1 before offline mode applied. The download completed; it is disclosed in summary_v2.md. Future cache misses stop for dependency approval.

## Migration / Compatibility Notes

Mode: adopt. Existing application ID/namespace `com.kinderman.sdo`, min SDK 30, compile/target SDK 37, Java 25. Module :app at app/; source roots app/src/main/java, app/src/test/java, app/src/androidTest/java. Resources/manifest at app/src/main; root catalogs/ is explicitly added as JVM resources. No Java-only/custom code modules, included builds, convention plugins or projectDir remaps are declared in settings. AppDatabase declares schema 28; no migrations or payload contracts change.

DI composition: SdoApplication + ViewModelFactories. Navigation destination/graph/host owner: presentation/SdoApp.kt, AppSurface and saved string back stack, not Navigation Compose. Argument maps remain unresolved, not empty-contract approval. Main entry: MainActivity.

Installed tools: Python C:/Users/kindb/Python/python.exe (3.8.5), Git Bash C:/Program Files/Git/bin/bash.exe, JBR C:/Program Files/Android/Android Studio/jbr (25.0.4), SDK C:/Users/kindb/AppData/Local/Android/Sdk with android-37.0 and platform-tools. Declared AGP/Kotlin/KSP/Compose/Room/Firebase pins are in spec_v1.md; retain them until source-backed compatibility results justify a separate proposal.

## Out of Scope

New product behavior, UI implementation, architecture migration, rule exceptions, protected instruction changes, dependency upgrades/installations, Firebase deployment or production data access, release signing/publishing, generated catalog updates and existing memory edits.

## Approval bindings

The following SHA-256 bindings identify the exact specification, test plan, profile and capability drafts submitted together. Consent applies only to this baseline scope. The actual plan hash is recorded in approval.json after consent, avoiding self-referential hashing.

<!-- ONBOARDING_BINDINGS -->

| Artifact | SHA-256 |
|---|---|
| docs/current/spec_v1.md | 0c394dd248b64d25e90894bce6ace5d880b35f0f430663e0600e21279ea0bb48 |
| docs/current/test_plan_v1.md | 3b02be6e4d384633df49f7a5876cc2d9b66776079d2a8fec5acd9c0a114c124f |
| harness-project.json | 43d2add63343583a6bffa69f30ece686e57792187115d0f06e80185616d449db |
| docs/product/product.md | da1b9f69c47d3838b0dbeed45b38b079b7c8e8166d387f734a38d1042814adf5 |
| docs/product/design_system.md | b7679607ad4ce8def099c6655a1af0a430c2930314c4e67409f926248929ef36 |
| docs/product/requirements/README.md | 3a7c7c89d2e8812c0b450249be5ab7c388303ae07033fe20fab3d125d0367b3e |
| docs/product/requirements/character-sheets.md | 3c3231b2b58e0d2585daa0f25d44b9273256fe68065375316ed6a6007fb514e0 |
| docs/product/requirements/campaigns.md | 0f341cc1e14d5186a688826b3358ca259c6cc1a076b5fc9dddd53bbda0230e5a |
| docs/product/requirements/codex.md | 147aeed48837d39589156a527cb31467791262a449ae9a803430f6f48e2d26c8 |
