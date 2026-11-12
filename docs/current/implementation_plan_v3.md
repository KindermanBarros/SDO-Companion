# SDO Companion harness setup — plan v3

Date: 2026-11-12. Supersedes v2 for remaining setup work. Covered by `docs/onboarding/setup-authorization.json`, which quotes the owner's prospective approval of all remaining setup changes/states. No further approval stop is required and no individual review of this revision is claimed.

## Feature / Bug

Finish project-specific Windows harness configuration and verify the real Android baseline.

## Requirement summary

Adopt the existing app while repairing setup-environment failures. Preserve application behavior and expose existing debt through enforced gates. Application source changes are not necessary for the current tooling repairs and are not bundled as incidental refactoring.

## Rule Applicability Implementation

Canonical decisions: [spec_v2.md#rule-applicability](spec_v2.md#rule-applicability).

| Required rule | Work/evidence |
|---|---|
| ARCH | Keep real module/layer mappings; H-03 isolated fixture checks; document policy gaps |
| IMPL | Configurable real instrumented runner and local environment, no placeholder callbacks |
| TEST | test_plan_v2.md H-01–H-07, Gradle XML, runtime receipts and gate logs |
| SEC | Official signed Python runtime, validated Java identifiers, unchanged production security/signing configuration and security checker contracts |

## Impact summary and exact file scope

| Files | Work |
|---|---|
| `build.gradle.kts`, `app/build.gradle.kts` | Pinned Ktlint/Detekt/Kover plugins and default enforced configuration |
| `.gitignore` | Ignore local harness runtime/evidence and Python bytecode; source/contracts remain version controlled |
| `harness-project.json`, `harness/templates/project-profile.example.json` | Explicit instrumentation IDs; updated real command/path contracts |
| `harness/scripts/project_profile.py` | Validate and emit instrumentation contract without shell eval |
| `harness/scripts/run-android-instrumented-tests.sh` | Consume app-specific IDs and HARNESS_PYTHON, preserve failure/zero-test/package checks |
| `harness/scripts/check-product-docs.py` | Decode UTF-8 explicitly on Windows |
| `harness/scripts/tests/android-instrumented-runner-contract-test.sh`, `product-docs-contract-test.py`, `project-onboarding-contract-test.py`, `ast-checkers-contract-test.sh` | Focused regressions and isolated fixture ownership |
| `harness/scripts/windows-env.ps1`, `setup-windows.ps1` | New process-local tool configuration and signed portable-runtime setup; no global PATH or registry changes |
| `harness/scripts/check-full-source-rules.ps1`, `.cmd` | Native launcher delegates to the full canonical bundle instead of omitting required contract checks |
| `docs/current/spec_v2.md`, `test_plan_v2.md`, `implementation_plan_v3.md` | Updated setup contract and bindings |
| `docs/onboarding/approval.json`, `setup-authorization.json` | Actual scoped consent and prospective authorization basis |
| `docs/current/summary_v2.md`, `onboarding-summary.md`, `docs/onboarding/discovery-review.md` | Actual stage/results/evidence reconciliation |
| `docs/changes/harness-retro-2026-11-12-sdo-windows-setup/retrospective.md`, `docs/knowledge/pitfalls/windows-consuming-app-harness.md` | Root-cause audit and reusable lesson |
| `.harness-state/runtime/`, `.harness-state/onboarding-baseline-v2/` | Ignored portable runtime, receipt/log/report artifacts |

Protected instruction file edit list: none. Existing AGENTS.md/README.md edits are preserved. Application source, test behavior, data schemas, catalogs and memory remain unchanged. Harness source/fixture mismatches require a draft PR with the actual verification limits recorded; no merge or release is authorized.

## API changes

None; no application endpoint, DTO or OpenAPI changes.

## UiState design

No new application states or UX implementation. Existing visual references stay authoritative. No Pen/visual acceptance claim.

## Test plan

[test_plan_v2.md](test_plan_v2.md). Portable Python 3.14.10 from python.org; executable Authenticode signature must identify Python Software Foundation. Use existing Git Bash/JBR 25/SDK and existing AVD. Pin quality plugins 14.2.0 / 2.0.0-alpha.6 / 0.9.11 from their official Gradle/plugin sources. Downloads are covered by the owner's later setup authorization. Existing cache misses are now resolvable without another approval request.

## Risks and compatibility

No policy exceptions or suppression baselines. A passing build does not settle existing architecture/Compose/localization/navigation violations. The original instrumentation runner's Notes app package and hardcoded Python command are harness defects; repair them without pretending legacy values are SDO runtime evidence. Initial Windows Python 3.8 and process-default text encoding are environment prerequisites/bugs, not application defects. Kover includes remaining legacy Compose packages conservatively rather than excluding ViewModels to inflate coverage.

## Files to delete and cleanup

None. Successful full verification may clean only unchanged manifest-owned temporary files. Preserve installed runtime, user-edited prompt, approvals, maps, evidence and shared caches. Stop only the emulator process started by this setup after evidence collection; do not wipe its data.

## Out of scope

New product features, UI redesign, business logic or data migrations, policy bypasses, production Firebase operations, release signing changes, publishing or merging the PR.

## Approval bindings

Record exact current spec/test/profile/capability hashes below and in approval.json under the prospective authorization basis. Do not overwrite historical v2 approval as if it covered the new hashes at the original timestamp.

<!-- ONBOARDING_BINDINGS -->

| Artifact | SHA-256 |
|---|---|
| docs/current/spec_v2.md | cf1fc5419ea2d656bc9776f68a45945e84da9c787949c5adb8586bc2f0c220e3 |
| docs/current/test_plan_v2.md | 62095da28c8f5375ed7d31de366c2f5b8de265218605564456d7cd0ebeb83613 |
| harness-project.json | 4da214fe2c8ef1878791d882528b121302c14472128f68d60668636e31f8d5d4 |
| docs/product/product.md | da1b9f69c47d3838b0dbeed45b38b079b7c8e8166d387f734a38d1042814adf5 |
| docs/product/design_system.md | b7679607ad4ce8def099c6655a1af0a430c2930314c4e67409f926248929ef36 |
| docs/product/requirements/README.md | 3a7c7c89d2e8812c0b450249be5ab7c388303ae07033fe20fab3d125d0367b3e |
| docs/product/requirements/character-sheets.md | 3c3231b2b58e0d2585daa0f25d44b9273256fe68065375316ed6a6007fb514e0 |
| docs/product/requirements/campaigns.md | 0f341cc1e14d5186a688826b3358ca259c6cc1a076b5fc9dddd53bbda0230e5a |
| docs/product/requirements/codex.md | 147aeed48837d39589156a527cb31467791262a449ae9a803430f6f48e2d26c8 |
