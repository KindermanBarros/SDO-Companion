# HISTORY

> **Sealed.** Edit only when the user explicitly asks.

[Memory](../docs/MEMORY.md) is the **single** canonical history and task-summary log of all agent modifications and task states.

## Rules

- Never create separate history or task-state files under `.docs/HistoryRecords/`, `.docs/OngoingTasks/`, or `.docs/CompletedTasks/`.
- Update the matching task summary row/section in [Memory](../docs/MEMORY.md) when a task starts, enters `InDevReview`, or is accepted as `Done` (documentation-only changes included).
- Use actual `YYYY-MM-DD HH:MM` timestamps in `America/La_Paz`.
- Record objective, date, time, changed scope, verification results, and checks not run concisely inside [Memory](../docs/MEMORY.md).
