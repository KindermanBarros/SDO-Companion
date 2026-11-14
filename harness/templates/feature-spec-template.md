# Feature Spec — <Feature Name>

- **Date / Status**: `YYYY-MM-DD` / `Draft | Final`
- **Related UI contract**: `UI_contract.md` *(when UI changes are in scope)*

## Objective & Scope
- **Objective**: `<What is being built, for whom, and user value>`
- **User Goal**: As a `<user>`, I want to `<action>` so that `<outcome>`.
- **In Scope**: `<capabilities>`
- **Out of Scope**: `<explicit non-goals>`

## Rule Applicability
Use `Required`, `Not applicable — <reason>`, or `Exception — approved by <user/date>`. Downstream plans link `#rule-applicability`.

| Rule ID | Rule document | Decision | Feature-specific evidence or reason |
|---|---|---|---|
| ARCH | `android-architecture.md` | `<decision>` | |
| IMPL | `implementation-rules.md` | `<decision>` | |
| TEST | `testing-strategy.md` | `<decision>` | |
| SUI | `compose-rules.md` | `<decision>` | |
| L10N | `localization-rules.md` | `<decision>` | |
| NAV | `navigation-rules.md` | `<decision>` | |
| API | `api-contract-rules.md` | `<decision>` | |
| OBS | `observability.md` | `<decision>` | |
| ANL | `analytics-rules.md` | `<decision>` | |
| SEC | `android-security.md` | `<decision>` | |

## Technical & Functional Requirements
- **Dependencies / Services / Constraints**: `<libraries, Firebase/catalogs/Room, Min SDK 30, permissions>`
- **FR-001**: System MUST `<specific capability>`.
- **AC-001**: Given `<state>`, when `<action>`, then `<observable result>`.
- **Data & Persistence**: `<Room, DataStore, catalog JSON, Firebase, or None>`
- **Edge Cases**: `<edge case and required behavior>`

## Screen States & Navigation *(include when UI changes)*

| State | Requirement | Acceptance Criteria |
|---|---|---|
| Loading / Empty / Content / Error | `<requirement>` | `<AC IDs>` |

- **Navigation**: Entry `<route>`, Back/cancel `<behavior>`, Success/Error recovery `<destination/state>`.

## Assumptions, Open Questions & Verification
- **Assumptions**: `A1: <assumption> (Risk if wrong: <impact>)`
- **Open Questions**: All questions must be `✅ Answered` before approval.
- **Verification**: Unit (`<scope>`), Integration (`<scope>`), Instrumented/Manual UI (`<scope>`).
