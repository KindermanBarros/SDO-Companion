# SDO Windows harness setup retrospective

Date: 2026-11-12. Authorization: `docs/onboarding/setup-authorization.json`.

## Incident and classification

Primary classification: HARNESS_ENVIRONMENT. The consuming app uses com.kinderman.sdo and AndroidJUnitRunner, but the instrumented runner selected the sample Notes app and Hilt runner. The Windows source launcher duplicated only part of the mandatory bundle. Documentation validation relied on a Windows locale default despite UTF-8 repository files. The AST fixture inherited SDO layer mappings instead of isolating its own fixture root.

Python 3.8.5 lacking pathlib.is_relative_to is a separate setup prerequisite; no compatibility shim was added. The application source, schemas and runtime behavior were not changed to make the harness pass.

## Evidence and invariants

- `.harness-state/onboarding-baseline-v2/runner-profile-red.log`: `FAIL: expected instrumentation component is not installed` for the unrelated Notes app. The runner must consume a validated explicit app/test/runner contract when a profile exists, and still reject failed, empty and missing-package results.
- `.harness-state/onboarding-baseline-v2/product-encoding-red.log`: `charmap codec can't decode byte 0x9d` (apostrophe wording omitted here). Repository document validation must decode UTF-8 explicitly, independent of process locale.
- `.harness-state/onboarding-baseline-v2/onboarding-contract.log`: `command unexpectedly succeeded` for a real fixture architecture violation. Fixture project roots must not inherit the consumer's profile/layers.
- Original `check-full-source-rules.ps1` omitted HTTP-boundary checks and negative security contracts. The native launcher must delegate to the same complete canonical bundle as Bash and preserve its failure exit.

## Changes

`project_profile.py` validates qualified instrumentation identifiers and emits them without shell eval. `run-android-instrumented-tests.sh` reads those values and honors HARNESS_PYTHON during XML generation. `check-product-docs.py` explicitly decodes UTF-8. The AST fixtures pass their own project roots. The PowerShell/.cmd launcher delegates to the Bash bundle.

Pinned Ktlint, Detekt and Kover plugins are configured with enforced defaults and no debt baselines, ignoreFailures or rule suppressions. Process-local Windows environment setup and the signed Python portable runtime avoid registry/global PATH changes. The runtime bootstrap is rerunnable and preserves unexpected/incomplete directories for inspection.

## Verification

- Runner contract: exit 0, profile-aware success plus failed/zero-test/missing-package rejection; log `runner-profile-green.log` in the run directory.
- UTF-8/documentation contracts: exit 0, 15 tests; `product-contract-green.log`.
- Onboarding contracts including profile identifier negatives: exit 0, 35 tests; `onboarding-contract-green.log`.
- AST fixture contract: exit 0; `ast-contract-green.log`.
- App JVM suite: exit 0, 241 tests, zero errors/failures/skips; `unit-online.log` and `unit-counts.json`.
- Real API 37 emulator migration: exit 0, one named SQLite migration test; `instrumented.log` and instrumented XML. Both SDO packages remained installed.
- Android Lint: 0 errors, 12 warnings, 2 hints; `app/build/reports/lint-results-debug.txt`.
- Full source, Ktlint and Detekt correctly reject existing application debt. Coverage is 61.24% (25407/41490), below the unchanged 80% requirement. These are not passes; no complete onboarding or cleanup is claimed.

## Routed application work and remaining risk

The initial full-source baseline reports 84 architecture, 227 Compose, 463 localization and 3 navigation violations. Manual DI/custom navigation/domain Android dependency need deliberate application decisions and maintenance. Default style/complexity checks reveal additional legacy debt. Fixing that behavior/architecture is not disguised as a harness validator relaxation. Runtime evidence here establishes one Room boundary, not complete UI, authentication, backend or navigation acceptance.

[Draft PR #141](https://github.com/KindermanBarros/SDO-Companion/pull/141) records the harness source/gate repairs and verification limits. No release, deployment, merge, production-data operation, catalog generation or cleanup is performed.
