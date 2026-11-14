---
name: knowledge-capture
description: Record high-value ADRs, post-mortems, or pitfalls after resolving non-obvious work.
---
# Skill — Knowledge Capture

Follow [documentation-and-adrs](../documentation-and-adrs/SKILL.md) and [product-documentation.md](../../rules/product-documentation.md).

## High-value filter
Ask: *"Would a developer or agent lose significant time without this record?"* If **no**, write zero knowledge files — routine bugs and self-evident code changes need only the standard `.docs/HistoryRecords/history_records.md` entry and task summary.

## When warranted
- **Architecture trade-off**: Append to [.docs/ArchitectureDecisions/architecture_decisions.md](../../../.docs/ArchitectureDecisions/architecture_decisions.md) (or `docs/knowledge/architecture-decisions/` when scoped there).
- **Non-obvious systemic bug**: Write `docs/knowledge/past-bugs/<YYYY-MM-DD>-<slug>.md` using [regression-template.md](../../../harness/templates/regression-template.md).
- **Reusable trap / pitfall**: Write `docs/knowledge/pitfalls/<slug>.md`.
