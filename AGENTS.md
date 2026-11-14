# AGENTS.md - SDO Companion
> **Sealed.** Edit only when the user explicitly asks.
Android API 30+ companion for character sheets, campaigns and the SDO codex; confirmed target: smartphone.

## Workflow
1. Read this file once. Adopt [PERSONAS](.docs/PERSONAS.md); load only skills needed for the current stage.
2. Read the [living backlog](docs/product/product.md#backlog-de-produto); select the requested item, preserve other priorities, and record new findings there.
3. Resume/create one [task state](.docs/TASK_PLANNING.md). For bounded work, a short scope/files/checks plan is enough.
4. Implement the smallest authorized change using [ARCHITECTURE](.docs/ARCHITECTURE.md) and [CODE_STANDARDS](.docs/CODE_STANDARDS.md).
5. Select checks once using [VALIDATION](.docs/VALIDATION.md). Reuse matching evidence; rerun only after a relevant change/failure.
6. Reconcile backlog and affected docs, append [history](.docs/HISTORY.md), remove .scratch, check the staged diff, commit and hand off InDevReview.

Ask only about unresolved product decisions, destructive ambiguity or missing authorization; reuse existing approvals.

## Documents
[SEALED] means edit only on explicit user request. Open other docs only when their Read when applies.

| Doc | Read when | Update when |
|---|---|---|
| [README](README.md) | Product context needed | User-visible feature, device or permission changes |
| [ARCHITECTURE](.docs/ARCHITECTURE.md) [SEALED] | Writing/moving/refactoring code | Explicit user request |
| [CODE_STANDARDS](.docs/CODE_STANDARDS.md) [SEALED] | Writing code | Explicit user request |
| [GIT_STANDARDS](.docs/GIT_STANDARDS.md) [SEALED] | Git action requested | Explicit user request |
| [PROJECT_CONTEXT](.docs/PROJECT_CONTEXT.md) | Locating a feature/refactor | Module responsibilities/communication change |
| [TECH_STACK](.docs/TECH_STACK.md) | Build/dependencies/SDK work | Those inputs or confirmed technology decisions change |
| [VALIDATION](.docs/VALIDATION.md) | Selecting checks or finishing | Check selection/commands change |
| [PERSONAS](.docs/PERSONAS.md) [SEALED] | Starting a task | Explicit user request |
| [SKILLS](.docs/SKILLS.md) | Selecting task skills | Installation/removal or project scope changes |
| [TASK_PLANNING](.docs/TASK_PLANNING.md) | Modifying files | Task-state index changes or explicit planning-policy request |
| [HISTORY](.docs/HISTORY.md) [SEALED] | Finishing a modification | Explicit user request; append audit records separately |

## Rules
- **Temporary files:** all agent-created throwaway files go under repository .scratch/. Remove the entire folder ALWAYS before every commit, including empty folders; never stage it. Promote necessary final evidence/contracts into docs/changes or docs/current first. Registered checkouts and standard tool-managed caches are infrastructure, not a scratch-file workaround.
- **Backlog:** docs/product/product.md is living workflow documentation: select work from it, add findings/requests, and reconcile status at handoff. Keep detailed intent in capability requirements; no unapproved scope or invented priorities.
- **Lean execution:** one owner for bounded tasks; delegate only when requested or when independent review/parallel work materially helps. No repeated planning, reapproval or validation for unchanged scope.
- **Context/output:** use rg and bounded reads; do not dump whole status/logs or reread loaded docs. Store one durable summary with commands/results/limitations; link instead of copying. Routine final output: result, check status, next action/PR.
- **Validation:** honor explicit owner build-only/manual-test preferences; report omitted checks, never fabricate acceptance. Security, data-loss and changed runtime boundaries still need appropriate evidence before acceptance.
- **Git:** scoped signed-off commits after applicable validation; branches/push/PR only when requested. Pre-commit scratch enforcement: .githooks/pre-commit; enable with git config --local core.hooksPath .githooks.
- **UI:** capture Android CLI/adb screenshots unless the owner explicitly owns runtime verification for that task.
- **APKs:** deliver only Firebase-configured builds signed with the registered certificate; missing inputs fail delivery.
- **Docs:** update only documents affected by the change; append one concise history record at handoff.
- **Time:** YYYY-MM-DD HH:MM America/La_Paz. Never invent versions, issue IDs or product decisions.
- **Done:** agent work ends InDevReview with honest evidence/limitations; only developer approval makes it Done.

## Document conventions
Root docs: SCREAMINGCASE.md; .docs main docs: SCREAMING_SNAKE_CASE.md; subfolders: PascalCase; subdocs: snake_case.md.
Link each subdoc from its parent. Keep this file <=60 lines and other maintained docs <=120; historical append-only audit records are exempt.
