# Sprint Contract Template

## Product Requirements
- Capability: `docs/product/requirements/<capability>.md`
- Affected requirement IDs / sections: `<changed behavior>`
- Approved requirements SHA-256: `<hash at approval>`
- Delta: `<proposed/approved behavior, or Confirmed unchanged with reason>`

## Sprint Overview & Scope
- **Sprint / Feature / Duration**: `{sprint-id}` / `{feature-name}` / `{sprint-duration}`
- **In Scope**:
  - [ ] `{In-scope capability 1}`
- **Out of Scope**:
  - `{Out-of-scope item 1}`

## Platform Capability & Rule Applicability Contract
Declare `platform_validation` in `feature_list.json` (`fail_loudly` when `required: true`). Copy the approved 10-row matrix (`ARCH`, `IMPL`, `TEST`, `SUI`, `L10N`, `NAV`, `API`, `OBS`, `ANL`, `SEC`) from `spec_v<N>.md#rule-applicability`:

| Rule ID | Rule document | Decision | Slice evidence |
|---|---|---|---|
| ARCH..SEC | `<rule-doc>.md` | `Required` / `Not applicable — <reason>` / `Exception — <approval>` | `<planned/recorded evidence>` |

## Production Journey Planning Contract *(required)*
Set **Yes** when any AC crosses a destination, navigation graph, back stack, saved state, or post-return boundary (`NAV: Required` requires at least one **Yes** row).

| User story | Journey required | Journey reason | Acceptance test ID | Production entry point | Planned test file & method | User actions & return boundary | Post-return assertion |
|---|---|---|---|---|---|---|---|
| US-1 | Yes / No | `{reason}` | `TC-US-1-01` or `N/A` | `SdoApp` / `MainActivity` or `N/A` | `app/src/androidTest/.../<Class>Test.kt#<method>` or `N/A` | `{real gestures + back/return}` | `{visible result}` |

## Spec Coverage Matrix *(required)*
Map every `FR-*`, `AC-*`, edge case, NFR, and design requirement to a user story and primary acceptance test (one AC and test per distinct outcome).

| Source requirement | Summary | Primary user story | Primary acceptance test | Handling |
|---|---|---|---|---|
| FR-001 / AC-001 | `{concise requirement}` | US-1 | TC-US-1-01 | In scope |

## User Scenarios & Acceptance Tests

### US-1: [Brief Title] (Priority: P1)
- **Independent Test**: `[How this slice is tested and delivers standalone value]`
- **Acceptance Criteria**:
  1. **AC-US-1-01**: Given `[state]`, When `[action]`, Then `[observable outcome]`

| Test ID | Covers AC | Test layer | Test file and method | Scenario / Fixture | Setup and action | Required assertions | Exact command |
|---|---|---|---|---|---|---|---|
| TC-US-1-01 | AC-US-1-01 | JVM unit / integration / Instrumented UI | `app/src/.../<Class>Test.kt#<method>` | `<scenario.json or N/A>` | Given `[fixture]`, when `[event]` | Assert `[state/output]` | `./gradlew :app:testDebugUnitTest --tests "<Class.method>"` |

*(Repeat `US-2`, `US-3`, ... in priority order. Execution records live in `feature_list.json` and `summary_v<N>.md`.)*
