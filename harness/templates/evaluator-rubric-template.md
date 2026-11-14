# Evaluator Rubric

| Category | Question | Score (0–5) | Notes |
|---|---|---:|---|
| Correctness | Does the implemented behavior match the approved requirement/slice? | | |
| Verification | Did the required build, test, and coverage checks run with fresh evidence? | | |
| Scope discipline | Did the session stay inside the approved slice scope? | | |
| Reliability | Does the result survive restart/rerun without manual repair? | | |
| Maintainability | Are code and documentation clear for the next session? | | |
| Handoff readiness | Can a fresh session continue from repository artifacts alone? | | |
| Code & Test Review | Do Ktlint, Detekt, Lint, code review, and test review pass? | | |
| Rule Applicability | Does every approved rule decision have diff reconciliation and evidence? | | |

### Overall: `<arithmetic mean of the 8 scores, rounded to 1 decimal>` / 5
- `5.0 / 5` -> `Accept` (ready for developer review / `InDevReview`).
- `< 5.0 / 5` -> `Revise` or `Block` (route back to fix).

## Hard Gates
- **Platform Gate**: Capability matrix linked when `platform_validation.required == true`, policy is `fail_loudly`, and real instrumented boundary tests pass (`Yes / No / N/A`).
- **Rule Applicability Gate**: All 10 rows reconciled in code and test reviews with valid evidence or approved rationale (`Yes / No`).
- **Visual / UI Gate**: When `requires_visual_verification == true` or UI changed, `ui_verification.json` and runtime/visual evidence match the approved `UI_contract.md` (`Yes / No / N/A`).

## Harness Artifact Check

| Artifact | Present | Quality | Notes |
|---|---|---|---|
| `feature_list.json` / task state | Yes / No | Complete / Partial | |
| `progress.md` / `session-handoff.md` | Yes / No | Complete / Partial | |
| `clean-state-checklist.md` | Yes / No | Complete / Partial | |

## Verdict & Follow-Up
- **Verdict**: `Accept` / `Revise` / `Block`
- **Missing evidence / Required fixes**: `<none or list>`
