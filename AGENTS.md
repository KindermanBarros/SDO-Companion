# AGENTS.md - SDO Companion
> **Sealed.** Edit only when the user explicitly asks.
Android API 30+ companion for character sheets, campaigns and the SDO codex; confirmed target: smartphone.

## Workflow
1. Read this file once. Adopt [PERSONAS](.docs/PERSONAS.md); load only skills needed for the current stage.
2. Read the [living backlog](docs/product/product.md#backlog-de-produto) and [Memory](docs/MEMORY.md); select the requested item, preserve other priorities, and record new findings there.
3. Resume/update the task summary in [Memory](docs/MEMORY.md) using [TASK_PLANNING](.docs/TASK_PLANNING.md); never create separate task-state or history files.
4. Implement the smallest authorized change using [ARCHITECTURE](.docs/ARCHITECTURE.md) and [CODE_STANDARDS](.docs/CODE_STANDARDS.md).
5. Select checks once using [VALIDATION](.docs/VALIDATION.md). Reuse matching evidence; rerun only after a relevant change/failure.
6. Reconcile backlog, affected docs and [Memory](docs/MEMORY.md), remove `.scratch`, check the staged diff, commit and hand off `InDevReview`.

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
| [TASK_PLANNING](.docs/TASK_PLANNING.md) | Planning/updating task status | Task-planning policy request |
| [HISTORY](.docs/HISTORY.md) [SEALED] | Finishing a modification | Explicit user request; summarize in [Memory](docs/MEMORY.md) |

## Rules
- **Temporary files:** all agent-created throwaway files go under repository `.scratch/`. Remove the entire folder ALWAYS before every commit, including empty folders; never stage it.
- **Single history & change files:** summarize all task states and history strictly in [Memory](docs/MEMORY.md); keep complex change specs/plans in one `docs/changes/<slug>.md` file. Never create separate files in `.docs/HistoryRecords/`, `.docs/OngoingTasks/` or `.docs/CompletedTasks/`.
- **Backlog:** `docs/product/product.md` is living workflow documentation: select work from it, add findings/requests, and reconcile status at handoff.
- **Lean execution:** one owner for bounded tasks; delegate only when requested or when independent review/parallel work materially helps.
- **Context/output:** use targeted reads; do not dump whole status/logs or reread loaded docs. Routine final output: result, check status, next action/PR.
- **Validation:** honor explicit owner build-only/manual-test preferences; report omitted checks, never fabricate acceptance.
- **Git:** scoped signed-off commits after applicable validation; branches/push/PR only when requested. Pre-commit hook: `.githooks/pre-commit`.
- **UI & APKs:** capture Android CLI/adb screenshots unless owner owns runtime verification; deliver only Firebase-configured signed APKs.
- **Time & Done:** `YYYY-MM-DD HH:MM America/La_Paz`. Never invent versions/IDs. Agent work ends `InDevReview`; only developer approval makes it `Done`.

## Document conventions
Root docs: SCREAMINGCASE.md; `.docs` main docs: SCREAMING_SNAKE_CASE.md; subfolders: PascalCase; subdocs: snake_case.md. Keep this file <=60 lines and other maintained docs <=120 (`docs/MEMORY.md` exempt).
