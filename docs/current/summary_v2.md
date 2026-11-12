# SDO Companion onboarding — stage evidence v2

Date: 2026-11-12. Current status: harness tooling configured; full onboarding acceptance blocked. The entries below preserve earlier baseline history; current authorization and final results are in `docs/current/onboarding-summary.md`.

## Scope confirmation

User replied “Ok” after the proposed adoption-only scope: adopt existing architecture, document gaps and propose specific repairs before migration. This is scope consent, not approval of unseen artifacts or policy exceptions.

## Stage evidence

| Stage | Status | Artifact and excerpt |
|---|---|---|
| Discovery | Complete | `docs/architecture/architecture-map.md`: `This is source evidence, not approved architecture policy.` |
| Requirements | Draft for approval | `docs/current/spec_v1.md`: `Adopt the existing Android application into the harness` |
| Implementation/test planning | Draft for approval | `docs/current/implementation_plan_v2.md`: `This plan authorizes local baseline verification and documentation reconciliation only.` |
| Implementation | Not started | No application changes authorized |
| Verification | Incomplete | `docs/onboarding/source-baseline.log`: `FAIL: full-source rules bundle (one or more checkers failed)` |
| Cleanup | Not eligible | Required gates and approvals remain unresolved |

Skill invocation interface is unavailable; repository project-onboarding, feature-specification and implementation-plan SKILL.md instructions were read directly. Stage gates are not bypassed.

## Baseline results

- Discovery using explicit Python: exit 0, one candidate module. `project_onboarding.py check`: exit 0, `PASS: project profile and architecture fingerprints`. These checks establish shape/freshness only.
- Source bundle with UTF-8: exit 1. Existing Compose, localization and navigation checks failed. Log: `docs/onboarding/source-baseline.log`, generated before the later profile mapping draft; do not reuse as a passing receipt for revised inputs.
- `adb devices`: exit 0, empty list. No instrumented runtime pass.
- Gradle diagnostic: `gradlew.bat --offline --version` exited 0 and reported Gradle 9.7.1 / Java 25.0.4. This is not a build or test result. The wrapper downloaded the 9.7.1 distribution before Gradle applied --offline. Download consent had not been obtained; the attempted interruption reached an already-completed command. No application files were changed. The cache is retained; no further downloads are authorized by this diagnostic.
- Build, JVM test count, coverage, Ktlint, Detekt and Android Lint: not run. Product-doc checker requires a newer Python than installed 3.8.5 because it uses pathlib.is_relative_to. No script compatibility workaround has been introduced.
- Initial empty-tracker validation: `bash harness/scripts/check-feature-lifecycle.sh --allow-empty` exited 0: `Feature lifecycle tracker valid: 0 feature(s), 0 in progress.` No complex feature status was advanced.
- Product documentation validation: `python harness/scripts/check-product-docs.py --change-dir docs/current --summary docs/current/summary_v2.md` exited 1 under Python 3.8.5: `AttributeError: 'WindowsPath' object has no attribute 'is_relative_to'`. This is a failed prerequisite, not accepted documentation evidence.
- Final profile freshness check after drafting the package exited 0: `PASS: project profile and architecture fingerprints`. Git status still shows the initial AGENTS.md/README.md changes; onboarding did not edit them.

## Product Requirement Reconciliation

New living capability drafts reflect setup_prompt.md; they do not change product behavior or claim runtime acceptance. The following intent statements remain unchanged during onboarding.

| Capability file | Existing claim reviewed | Disposition | Current requirement location or retired status | Evidence / qualification |
|---|---|---|---|---|
| docs/product/requirements/character-sheets.md | Players can create character sheets. | Confirm unchanged | SHEET-001 | Reason: setup_prompt.md directly requests sheet creation; runtime acceptance is still unverified |
| docs/product/requirements/campaigns.md | Players can use character sheets in campaigns. | Confirm unchanged | CAMPAIGN-001 | Reason: setup_prompt.md directly requests campaign use; source presence is not execution proof |
| docs/product/requirements/codex.md | The beta provides a codex for the user's RPG system. | Confirm unchanged | CODEX-001 | Reason: setup_prompt.md requests a codex; completeness and runtime behavior remain unverified |

## Remaining blockers

Actual approval of the bound package, supported Python for all gates, unresolved DI/navigation/state policy, source violations, quality/coverage task availability, dependency resolution and real Android runtime evidence. No approval.json has been created.

## Current stage reconciliation

The preceding blocker list is historical. The user subsequently approved plan v2 and preapproved remaining setup changes/states in `docs/onboarding/setup-authorization.json`; the current scope and canonical applicability are `spec_v2.md#rule-applicability`. Dependency resolution, Python compatibility, plugin availability and one real Room runtime boundary are now verified. Source/style/architecture debt and 61.24% coverage remain blockers.

| Stage | Current status | Artifact and one-line excerpt |
|---|---|---|
| Requirements | Covered by actual prospective authorization | `spec_v2.md`: `Make the installed harness usable with SDO Companion on Windows` |
| Implementation/test planning | Covered by actual prospective authorization | `implementation_plan_v3.md`: `Supersedes v2 for remaining setup work.` |
| Harness implementation | Changes in place; final acceptance blocked | `docs/changes/harness-retro-2026-11-12-sdo-windows-setup/retrospective.md`: `The runner must consume a validated explicit app/test/runner contract` |
| Harness contracts | Passing | `.harness-state/onboarding-baseline-v2/onboarding-contract-green.log`: `Ran 35 tests` |
| App verification | Mixed; not complete | `onboarding-summary.md`: `JVM suite` records 241 tests; `Full-source rules` records exit 1 |
| Cleanup / complete onboarding | Not eligible | `.harness-state/onboarding-baseline-v2/final-verifier.log`: `FAIL: resolve profile unknowns before verification` |
