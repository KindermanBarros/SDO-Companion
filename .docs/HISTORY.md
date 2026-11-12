# HISTORY

> **Sealed.** Edit only when the user explicitly asks.

[History records](HistoryRecords/history_records.md) is the append-only audit log of agent modifications.

## Rules

- Append one entry when a modification enters InDevReview; documentation-only changes count.
- Do not read, edit or delete prior records. Initial setup may populate template fields to establish the baseline.
- Use actual YYYY-MM-DD HH:MM timestamps in America/La_Paz.
- On POSIX: TZ=America/La_Paz date '+%Y-%m-%d %H:%M'.
- Record objective, date, time, changed scope, verification results and checks not run.
- A later review revision gets a new entry.
