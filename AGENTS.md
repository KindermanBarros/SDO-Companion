# AGENTS.md — SDO Companion

> **Sealed.** Edit only when the user explicitly asks. If you think a rule here should change, propose it in your summary.

SDO Companion is an Android API 30+ RPG companion for character sheets, campaigns and the SDO codex; confirmed target hardware is a smartphone. Read this file fully; open other docs only when their "Read when" applies.

## Workflow

1. Adopt the persona for the task ([PERSONAS](.docs/PERSONAS.md)).
2. If the task modifies files, plan it before editing ([TASK_PLANNING](.docs/TASK_PLANNING.md)).
3. Load every skill that matches the task ([SKILLS](.docs/SKILLS.md)).
4. Implement following [ARCHITECTURE](.docs/ARCHITECTURE.md) and [CODE_STANDARDS](.docs/CODE_STANDARDS.md).
5. Validate ([VALIDATION](.docs/VALIDATION.md)).
6. Update every doc whose "Update when" applies, append a history record ([HISTORY](.docs/HISTORY.md)), and hand the task to the developer for review.

If a requirement or product decision is unclear, ask. Don't guess.

## Documents

[SEALED] = sealed: edit only when the user explicitly asks.

| Doc | Read when | Update when |
|---|---|---|
| [README](README.md) | You need product context | A user-visible feature, target device or permission changes |
| [ARCHITECTURE](.docs/ARCHITECTURE.md) [SEALED] | You write, move or refactor code | The user explicitly asks |
| [CODE_STANDARDS](.docs/CODE_STANDARDS.md) [SEALED] | You write code | The user explicitly asks |
| [GIT_STANDARDS](.docs/GIT_STANDARDS.md) [SEALED] | **Only** when the user asks for a git action | The user explicitly asks |
| [PROJECT_CONTEXT](.docs/PROJECT_CONTEXT.md) | You decide where a feature or refactor goes | Modules, their responsibilities or their communication change |
| [TECH_STACK](.docs/TECH_STACK.md) | You touch dependencies, plugins, SDK levels or the build | Any of those changes, or a plan confirms a new technology |
| [VALIDATION](.docs/VALIDATION.md) | You write tests, or before you finish | The test pattern or the check commands change |
| [PERSONAS](.docs/PERSONAS.md) [SEALED] | You start a task | The user explicitly asks |
| [SKILLS](.docs/SKILLS.md) | You start a task | A skill is installed or removed, or the project scope changes |
| [TASK_PLANNING](.docs/TASK_PLANNING.md) | You receive a task that modifies files | A task-state file is created or deleted (§ Ongoing tasks only) |
| [HISTORY](.docs/HISTORY.md) [SEALED] | You finish a modification | The user explicitly asks |

## Rules

- **Git:** automatically commit completed changes after applicable validation, following GIT_STANDARDS; stage only the task's files and sign off. Create branches, push and open pull requests only when the user asks.
- **UI tasks:** screenshots are mandatory for UI tasks; capture them using Android CLI or adb commands.
- **Docs are part of done:** update them in the same change as the code.
- **Timestamps:** `YYYY-MM-DD HH:MM America/La_Paz`. On POSIX use `TZ=America/La_Paz date '+%Y-%m-%d %H:%M'`. Never write relative dates.
- **Never invent** versions, Jira IDs or product decisions. Read them from the source, or ask.
- **Your part is done** when the checks in VALIDATION pass, the docs are updated, the history record is appended and the task is `InDevReview`. Only the developer's approval moves it to `Done`. Your summary names what you couldn't run.

## Document conventions

- Root docs: `SCREAMINGCASE.md`. Main docs in `.docs/`: `SCREAMING_SNAKE_CASE.md`. Subfolders: `PascalCase/`. Sub docs: `snake_case.md`.
- A sub doc only details its main doc, and that main doc links to it. Don't create a sub doc without a parent. Add every new main doc to the table above.
- Keep AGENTS.md at most 60 lines and every other doc at most 120. Move longer sections into a linked sub doc.
