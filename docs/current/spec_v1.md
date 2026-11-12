# Feature Spec — SDO Companion adoption baseline

**Date**: 2026-11-12
**Status**: Draft for approval; adoption scope confirmed by the user's “Ok” after the scope question. Specification and implementation approval are pending.
**Related UI contract**: Not applicable; no UI change.

## Objective

Adopt the existing Android application into the harness with accurate configuration, product documentation, reproducible baseline evidence and explicit blockers.

## User Goal

As the project owner, I want setup to describe and verify my existing app so subsequent changes use its real architecture and do not silently rewrite it.

## Scope

### In Scope

Inventory the existing :app module, source sets, dependencies, navigation and service wiring; draft the project profile and product documentation; run authorized baseline checks; report exact policy gaps and propose separate repairs. User confirmed this scope on 2026-11-12.

### Out Of Scope

Application/test source changes, architecture migrations, policy exceptions, instruction changes, new features, catalog regeneration, visual redesign, production Firebase operations, signing changes, deployment and publishing. Approval of this baseline does not authorize repairing every violation found.

## Product Requirements

This onboarding records intent without changing application behavior:

- `docs/product/requirements/character-sheets.md`: SHEET-001 through SHEET-003.
- `docs/product/requirements/campaigns.md`: CAMPAIGN-001.
- `docs/product/requirements/codex.md`: CODEX-001.

These new capability drafts preserve the setup prompt's goals and distinguish static observations from verified delivery. Their exact SHA-256 values are bound in implementation_plan_v2.md for joint approval; no prior capability approval is claimed.

## Rule Applicability

| Rule ID | Rule document | Default | Decision for this change | Trigger / rationale | Planned evidence |
|---|---|---|---|---|---|
| ARCH | android-architecture.md | Always | Required | Reconcile actual module/layer ownership and DI with policy; no migration | OB-01, OB-02, reviewed source map and recorded gaps |
| IMPL | implementation-rules.md | Always | Required | Adopt real existing behavior; no source scaffold or dummy callbacks | Diff review proves no production source changes; OB-02 |
| TEST | testing-strategy.md | Always | Required | Baseline build and non-zero fresh test evidence; no false passes | OB-03 through OB-07, exact exit/count/report records |
| SUI | compose-rules.md | Conditional | Not applicable — no Compose implementation, layout, state or accessibility changes | Existing UI is inventoried only | Existing baseline findings remain open; full-source checks retained |
| L10N | localization-rules.md | Conditional | Not applicable — no user-visible copy, formatting, plural or label changes | Documentation only | Existing localization failures remain open |
| NAV | navigation-rules.md | Conditional | Not applicable — no route, destination, tab, argument or back-stack changes | Profile identifies existing owner; no navigation behavior accepted | Missing navigation contracts remain explicit blockers |
| API | api-contract-rules.md | Conditional | Not applicable — no endpoint, DTO, schema, error contract or OpenAPI changes | Existing Firebase SDK integration is recorded only | No new shared JSON endpoint case required |
| OBS | observability.md | Conditional | Not applicable — no async, network, persistence, recovery or diagnostic logging changes | Gate logs are setup evidence, not application logging | No application telemetry added |
| ANL | analytics-rules.md | Conditional | Not applicable — analytics: none | No new impressions, actions, funnels or business events | No application event changes |
| SEC | android-security.md | Conditional | Not applicable — no Android security boundary is changed | No authentication, storage, manifest, SDK, dependency or signing changes | Baseline execution must avoid production data; new boundary work requires revised applicability |

Not-applicable decisions describe the change delta; they do not waive repository-wide failures. Manual DI and custom navigation remain unresolved policy gaps, without exceptions.

## Technical Spec

### Libraries & Dependencies

Observed declarations: Gradle 9.7.1; AGP 9.4.0; Kotlin/Compose/serialization plugins 2.4.20; KSP 2.3.12; Compose BOM 2026.09.00; Firebase BOM 34.19.0; Room 2.8.5. Retain existing pins. This specification authorizes no upgrades or new application libraries.

### Key Technical Decisions

Adopt existing Kotlin/Compose/Material 3/Room/Firebase application. SdoApplication uses manual composition and ViewModelFactories; SdoApp uses custom AppSurface navigation. Record differences from Hilt, single-UiState and Navigation Compose policies. Existing architecture is observed, not approved as an exception. Root catalogs is an additional JVM resources directory, not an external code module. No projectDir remaps or convention builds are declared in settings.

### External APIs / Services

Firebase Auth and Firestore already exist. Baseline commands must not deploy or write production records. Gradle dependency downloads require approval; no Firebase CLI installation is needed to document the app.

### Platform & Compatibility Constraints

Application ID/namespace com.kinderman.sdo; min SDK 30, compile/target 37, Java 25. Existing INTERNET permission remains unchanged. Missing SDKs, credentials or devices are blockers rather than reasons to alter SDK versions or skip evidence.

## Functional Requirements

- **FR-001**: Record actual module, source/resource roots and command arrays with source-backed architecture ownership.
- **FR-002**: Preserve existing app, tests, catalogs, instructions, edited prompt, memory and unrelated user work.
- **FR-003**: Separate command execution, source observations and runtime proof; retain failures and unresolved policy gaps explicitly.
- **FR-004**: Require exact plan/profile approval and additional required gates before successful onboarding or temporary cleanup.

## Acceptance Criteria

- **AC-001**: Given the existing repository, profile validation recognizes :app and every declared source root, including presentation/UI mapping and catalog resources documented in the plan.
- **AC-002**: Given baseline execution, the diff contains no application, test, catalog, signing, protected instruction or existing memory changes attributable to onboarding.
- **AC-003**: Given any failed source/build/test command, the summary records its exit and evidence location and does not mark onboarding complete.
- **AC-004**: Given a passing unit-test command, only fresh XML with non-zero executed tests, no failures/errors/skips establishes a pass.
- **AC-005**: Given missing navigation contracts, quality/coverage tasks, runtime, dependencies or unresolved architecture policy, those gaps remain blocking or explicitly unverified; no exclusions or invented exceptions are introduced.
- **AC-006**: Given eventual successful onboarding verification, only unchanged manifest-owned temporary files may be cleaned; runtime harness, prompt, approvals and evidence remain.
- **AC-007**: Given plan/profile changes, prior approval hashes cannot be reused; actual user approval must match the exact revised inputs.

## Data And Persistence

Only local onboarding docs/profile/evidence are authored. Existing Room schemas, migrations, Firebase records and user preferences are preserved. Shared dependency caches are outside setup cleanup ownership.

## Edge Cases

Windows Store Python aliases require an explicit installed interpreter. Python 3.8 can execute discovery but is insufficient for scripts using pathlib.is_relative_to; a supported Python runtime is a setup dependency, not a reason to patch checks. Set UTF-8 for Python subprocess output. Gradle wrapper bootstrap may download before --offline takes effect; check cache or obtain download approval first. An empty adb device list cannot support runtime acceptance.

## Explicit Assumptions

None needed for the confirmed adoption-only scope. Unverified technical facts remain profile unknowns; product or migration choices are not inferred.

## Open Questions

| Question | Status | Answer |
|---|---|---|
| Adopt existing architecture and propose repairs separately? | Answered | User “Ok” confirmed this scope; no exception or repair authorization inferred |

## Verification Expectations

Test plan `test_plan_v1.md` defines static validation, baseline build/JVM tests and real-runtime prerequisites. Existing runtime test candidates provide no acceptance until executed against an identified runtime. No visual parity or complete-app claim is part of this change.

## No Open Questions Gate

- [x] Requirements are specific and testable for adoption scope.
- [x] Non-goals are explicit.
- [x] No unresolved product assumptions.
- [x] UI and navigation behavior changes are outside scope.
- [ ] User approval of this specification is recorded.
