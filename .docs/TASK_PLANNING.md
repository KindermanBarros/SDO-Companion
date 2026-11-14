# TASK_PLANNING
Plan before editing; read-only inspection needs no task update. Summarize all ongoing and completed tasks inside [Memory](../docs/MEMORY.md) instead of creating separate task-state files.

## Proportional planning
- Select the requested item from the [living backlog](../docs/product/product.md#product-backlog); link its existing ID or record the direct owner request.
- For bounded fixes, keep a short inline scope/files/checks plan and update [Memory](../docs/MEMORY.md) at handoff; no duplicate spec/approval loop.
- Significant features/architecture/data changes use the owning workflow and record decisions/evidence in [Memory](../docs/MEMORY.md) (optional binary assets under `docs/changes/assets/`).
- Never create separate history/change files under `docs/changes/*.md`, `.docs/OngoingTasks/`, `.docs/CompletedTasks/`, or `.docs/HistoryRecords/`.
- Throwaway files belong only in `.scratch`, which must be absent before every commit.

## States
| State | Meaning |
|---|---|
| ToDo | Planned |
| InProgress | Executing |
| InAgentValidation | Selected checks running |
| InDevReview | Agent handoff; update [Memory](../docs/MEMORY.md) and reconcile backlog |
| Done | Developer-approved only |

Only developer approval moves work to `Done` (update its status row in [Memory](../docs/MEMORY.md) and reconcile `docs/product/product.md`).
