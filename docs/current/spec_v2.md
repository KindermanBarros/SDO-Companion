# SDO Companion — harness setup completion contract

Date: 2026-11-12. Covered by the owner's prospective setup authorization in `docs/onboarding/setup-authorization.json`; no claim of individual review of this revision.

## Objective

Make the installed harness usable with SDO Companion on Windows and measure its actual application baseline without changing product behavior or disguising failing gates.

## User goal

The owner can run project-specific discovery, quality, coverage and Android verification using the existing app and real evidence.

## Scope

Configure local tools, build quality/coverage plugins and consuming-app instrumentation identifiers; repair Windows encoding and isolated harness fixtures; document results and route application debt. No application logic, UI redesign, schema, catalog or Firebase deployment changes. Existing user edits remain preserved.

## Product Requirements

Existing living requirements remain unchanged: `docs/product/requirements/character-sheets.md` (SHEET-001–003), `docs/product/requirements/campaigns.md` (CAMPAIGN-001), `docs/product/requirements/codex.md` (CODEX-001). No requirement is promoted to runtime-accepted solely because a build passes. Approval-time hashes are bound by implementation_plan_v3.md.

## Rule Applicability

| Rule ID | Rule document | Decision | Trigger / planned evidence |
|---|---|---|---|
| ARCH | android-architecture.md | Required | Actual :app roots/composition; profile validation, original architecture gaps remain visible |
| IMPL | implementation-rules.md | Required | Real configurable runner and tool environment; no application callbacks authored |
| TEST | testing-strategy.md | Required | Harness contract tests, fresh Gradle XML, real migration test and source/quality/coverage gates |
| SUI | compose-rules.md | Not applicable — no Compose implementation, visual or interaction changes | Existing full-source findings remain blockers |
| L10N | localization-rules.md | Not applicable — no application copy changes | UTF-8 fix concerns documentation file decoding only |
| NAV | navigation-rules.md | Not applicable — no application route or back-stack changes | Existing missing navigation contracts remain blockers, not exceptions |
| API | api-contract-rules.md | Not applicable — no endpoint, DTO, schema or OpenAPI changes | Firebase SDK contracts remain unchanged |
| OBS | observability.md | Not applicable — no application async/error/logging boundary changes | Setup command logs are verification artifacts |
| ANL | analytics-rules.md | Not applicable — analytics: none | No product analytics scope |
| SEC | android-security.md | Required | Downloaded runtime/build tooling and validated runner identifiers; official sources, signature check, no production data or release signing changes, existing security contracts |

## Technical spec

Retain the existing Android toolchain/SDK/application pins. Add Ktlint Gradle plugin 14.2.0, Detekt 2.0.0-alpha.6 and Kover 0.9.11 to root/app build declarations with enforced defaults; no baselines, ignoreFailures, disabled rules or debt exclusions. Keep the approved structural screen/components coverage exclusion only. Download the official Python 3.14.10 embeddable runtime into ignored local state, verify its executable signature, configure script imports locally, and avoid registry/global PATH changes. Process-local environment uses existing JBR 25, Git Bash and Android SDK. Test on the existing API 37 AVD when available; never wipe user device data.

## Functional requirements

- SETUP-01: Runner uses validated project-specific application/test IDs and AndroidJUnitRunner; absent/invalid profile contracts fail before installation.
- SETUP-02: UTF-8 documentation validates regardless of Windows process-default encoding.
- SETUP-03: Harness fixture roots are isolated from the consuming app's profile, so actual violations are rejected.
- SETUP-04: Quality and coverage tasks execute against the application's real source; missing tasks, failed checks and zero tests never count as passing.
- SETUP-05: Baseline results include exact commands, exits, test counts, fingerprints, logs and remaining adoption blockers.

## Acceptance criteria

- A1: Existing and foreign-package runner fixtures pass; failed/empty/missing-package fixtures remain rejected, and malformed instrumentation IDs are rejected by profile validation.
- A2: The Unicode fixture fails before the decoding repair and passes afterwards even with `-X utf8=0`; all documentation contract tests pass.
- A3: Actual architecture fixture violations fail for the intended diagnostic while comment-only fixtures pass, with and without a consuming project profile.
- A4: JVM XML is fresh, non-zero, failure/error/skip-free before recording a pass; runtime evidence names the executed real migration method.
- A5: Source, quality and coverage failures remain blockers; configuration alone never becomes full onboarding acceptance.
- A6: Only successful complete verification permits installer-owned temporary cleanup. Runtime tools, edited prompt, approval and evidence remain.

## Data and persistence

No production Firebase writes, new app schemas, signing material or catalog generation. Setup caches and logs live in `.harness-state/`; persistent configuration/docs stay in the repository. The existing emulator is used without a wipe.

## Edge cases

Default python3 is a Windows Store alias; shell scripts honor HARNESS_PYTHON. Portable Python's isolated mode ignores UTF-8 environment switches, so validators decode UTF-8 explicitly. PowerShell local script execution uses a child process with RemoteSigned policy only; machine/user policy is unchanged. A configured source gate may correctly reject existing app debt.

## Assumptions and open questions

No unresolved product choice is needed. The owner authorized all remaining harness setup changes and state transitions without additional approval stops. This does not authorize fake passing evidence, production deployment or silent architecture exceptions.

## Verification expectations

Use test_plan_v2.md. Report harness configuration progress separately from application baseline acceptance. Manual DI, domain Android dependency and custom navigation are not accepted exceptions merely because setup was preapproved.
