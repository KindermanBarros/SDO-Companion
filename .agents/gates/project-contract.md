# SDO harness contract

Read [AGENTS](../../AGENTS.md) first. This document owns integration between workflows, role profiles and the populated harness.

## Sources and scope

- [ARCHITECTURE](../../.docs/ARCHITECTURE.md) records observed architecture; [CODE_STANDARDS](../../.docs/CODE_STANDARDS.md) governs code.
- [PROJECT_CONTEXT](../../.docs/PROJECT_CONTEXT.md) owns module/package responsibilities; [TECH_STACK](../../.docs/TECH_STACK.md) owns declared versions.
- SDO has one :app module, package com.kinderman.sdo, smartphone target and API 30 minimum / 37 compile and target.
- Existing wiring is manual; navigation uses SdoApp/AppSurface; state uses several flows. Do not silently migrate to Hilt, NavHost or a single UiState.
- Generic rules in .agents/rules describe desired boundaries, not proof that source conforms. Report policy differences; changes require explicit scope/approval.
- Use [PERSONAS](../../.docs/PERSONAS.md) and [SKILLS](../../.docs/SKILLS.md). Persona TDD requirements apply when that persona is adopted; bugs always require RED before the fix.
- Read a stage skill completely. If no invocation interface exists, read its local SKILL.md and record the interface limitation once; executable gates still apply.
- Role assignments do not pin a model. Use available roles; preserve a fresh independent evaluator and report unavailable required delegation.

## Planning and records

Create/resume task state under .docs/OngoingTasks and index it in [TASK_PLANNING](../../.docs/TASK_PLANNING.md) before editing.
Task state records progress and links the approved execution contract; it does not duplicate feature scope.

| Record | Authority |
|---|---|
| Policy / technical baseline | Root AGENTS.md and .docs |
| Living product behavior | docs/product/requirements/ |
| Product capability index / complex lifecycle | docs/product/product.md |
| Ad-hoc spec, approved plan, tests, summary | docs/current/ |
| Complex spec, slices, sprint contract, evidence | Tracker-linked .scratch/ workspace |
| Ephemeral QA | .qa-artifacts/ run |
| Durable harness-change audit | docs/changes/ |

Preserve prior approvals and evidence. Do not infer approval from a folder, file existence or silence.
Reuse valid session authorization for its exact scope. Changed product/design/implementation scope returns to its owning approval stage.
Reconcile product claims through [Product documentation flow](../rules/product-documentation.md) only when behavior changes; harness-only work records behavior unchanged.

## Active verification

[CI checks](ci-checks.md) owns gate selection. [Review](review-checklist.md) owns review criteria.
A required command must exit zero with its actual evidence. Failed, absent, skipped or unavailable evidence is non-passing.
The owner retired gates backed by absent legacy scripts. They are not required and must not be reintroduced by workflows, skills or generic rules.
Only the current CI catalog defines executable gates; retain tests, evidence review and human approvals for the declared scope.
Do not restore deleted tooling, install dependencies or rewrite application policy merely to complete an unrelated documentation task.
Do not claim Roborazzi, mock libraries, Hilt or a CI job exists without configuration evidence.
Runtime claims follow [Runtime evidence](../rules/testing-runtime-evidence.md); fake-only tests cannot prove production boundaries.
Screenshots are mandatory for UI tasks; save the approved reference separately from defect/implementation captures.
APKs delivered for owner testing must be authenticated: use the existing protected-file restoration and signature verification actions, and fail if inputs are absent; no demo fallback.
Structural harness validation proves instruction integrity only. It is sufficient for documentation-only changes and does not certify the app.

## Handoff and git

Update affected docs and append [HISTORY](../../.docs/HISTORY.md) at InDevReview; only developer approval changes the task to Done.
Use America/La_Paz timestamps. Preserve unrelated edits, existing deleted tooling and historical acceptance blockers.
The owner prefers automatic commits after the applicable checks pass: stage only the completed task's files and use a signed-off commit.
Follow [Git standards](../../.docs/GIT_STANDARDS.md); no Jira ID may be invented. Do not create a branch, push, open a PR, merge or release unless requested.
A commit does not approve the change or imply application readiness. Blocked work stays non-passing.
