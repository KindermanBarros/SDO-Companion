# TASK_PLANNING
> **Sealed history policy.** Change history/lifecycle policy only on explicit owner request.

Plan before editing; read-only inspection needs no task update. Summarize all ongoing and completed tasks inside [Memory](../MEMORY.md) instead of creating separate task-state files.

## Proportional planning
- Select the requested item from the [living backlog](../product/product.md#product-backlog); link its existing ID or record the direct owner request.
- For bounded fixes, keep a short inline scope/files/checks plan and update [Memory](../MEMORY.md) at handoff; no duplicate spec/approval loop.
- Significant features/architecture/data changes use the owning workflow and record decisions/evidence in [Memory](../MEMORY.md) (optional binary assets under `docs/changes/assets/`).
- Use the [single history policy](#history-and-handoff); do not create separate history/task files.
- Throwaway files belong only in `.scratch`, which must be absent before every commit.

## States
| State | Meaning |
|---|---|
| ToDo | Planned |
| InProgress | Executing |
| InAgentValidation | Selected checks running |
| InDevReview | Agent handoff; update [Memory](../MEMORY.md) and reconcile backlog |
| Done | Developer-approved only |



## History and handoff

[Memory](../MEMORY.md) is the sole history, decision and task-summary log. Record objective, actual `YYYY-MM-DD HH:MM America/La_Paz`, scope, evidence and limitations in the existing task entry.
Update that entry at start and handoff; only developer acceptance moves it to Done. Separate history/task-state Markdown files are forbidden, including under changes/, HistoryRecords/, OngoingTasks/ or CompletedTasks/.
Preserve existing acceptance/provenance; record layout migrations without representing them as product or design approval.
