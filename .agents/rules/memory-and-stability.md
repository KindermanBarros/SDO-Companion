# Memory & Artifact Stability Rules

## 1. Single Memory & History File (`docs/MEMORY.md`)
- `docs/MEMORY.md` is the **only** canonical file for project history, task summaries (`InProgress` / `InDevReview` / `Done`), and accumulated context.
- **No separate task/history files**: Creating separate files under `.docs/HistoryRecords/`, `.docs/OngoingTasks/`, or `.docs/CompletedTasks/` is strictly forbidden.
- Every agent must consult `docs/MEMORY.md` at task start and update the corresponding task summary in `docs/MEMORY.md` at handoff (`InDevReview`) or completion (`Done`).

## 2. Lateral Immutability of Existing Artifacts
- Previously generated artifacts (production code, tests, specs, and docs outside the active task scope) are protected against lateral edits.
- Do not refactor, edit, or delete unrelated code/docs as a side effect of another task; report findings to the user instead of altering them silently.
