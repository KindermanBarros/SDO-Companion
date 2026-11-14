# Implementation Plan Template

## Feature / Bug & Summary
- **Goal**: `<1–3 sentences describing what is being built or fixed and why>`
- **Canonical Spec**: `spec_v<N>.md#rule-applicability`

## Rule Applicability Implementation

| Required Rule ID | Planned implementation work & verification evidence |
|---|---|
| `<RULE_ID>` | `<files, checks, and evidence>` |

## Impact & File Plan

| Layer | File Path | Action (`create` / `modify` / `delete`) | Purpose / Change |
|---|---|---|---|
| UI / Presentation | `app/src/main/java/com/kinderman/sdo/...` | `modify` | `<summary>` |
| Domain | `app/src/main/java/com/kinderman/sdo/domain/...` | `create` | `<summary>` |
| Data | `app/src/main/java/com/kinderman/sdo/data/...` | `modify` | `<summary>` |
| Tests | `app/src/test/java/com/kinderman/sdo/...` | `modify` | `<summary>` |

## Data / Schema / Contract Changes
- **Classification**: `none` / `backward compatible` / `breaking`
- **Details**: `<Room schema, catalog JSON, or Firebase contract changes and fallback behavior>`

## UiState & UI State Stages *(remove for non-UI work)*
- **States covered**: Loading, Content, Empty, Error, Retry.

| Stage ID | Implementation | Verification target & command |
|---|---|---|
| UI-01 | `<screen/state code change>` | `app/src/androidTest/.../<Feature>Test.kt#<method>` (`./gradlew :app:connectedDebugAndroidTest`) |

## Linked Test Plan, Assumptions & Risks
- **Test Plan**: [test-plan-template.md](test-plan-template.md) (`test_plan_v<N>.md`)
- **Explicit Assumptions**: `1. <assumption>`
- **Risks & Mitigations**: `1. <risk> — <mitigation>`
- **Out of Scope**: `<explicit non-goals>`
