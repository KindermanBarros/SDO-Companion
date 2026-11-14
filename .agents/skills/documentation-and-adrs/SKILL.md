---
name: documentation-and-adrs
description: Maintain living requirements, backlog, concise decisions and delivery records.
---
# Documentation and ADRs
Follow root AGENTS and [Product documentation flow](../../rules/product-documentation.md).

## Use
- Record decisions, requirements changes, delivery evidence or reusable pitfalls.
- Maintain the existing backlog at task selection and handoff; add findings without silently expanding scope.
- Do not write a new report merely because a workflow stage changed.

## Locations
- Product/backlog: docs/product/product.md; detailed behavior in docs/product/requirements/.
- Durable dated contracts/change evidence: docs/changes/YYYY-MM-DD-feature-name/.
- Active ad-hoc spec/plan/summary: docs/current/.
- Architecture decisions: use the repository's existing .docs architecture decision policy; do not create another ADR index.
- Temporary drafts/raw logs/experiments: .scratch only; promote final records and remove it before commit.

## Write economically
- Read only documents affected by the task; reuse context already loaded.
- State decision, reason, observed result and limitation once. Link canonical rules instead of copying them.
- Update stale claims in place and distinguish intent/source/verified behavior.
- A short task state and one summary suffice for a bounded change.
- Preserve audit records and approvals; append history rather than rewriting prior entries.
- Keep maintained docs <=120 lines; split only with a linked parent when necessary.
- Never invent versions, IDs, priorities, approvals or passing evidence.

## Validate once
Check affected links, line limits and scoped diff. Run harness regression only for changed enforcement.
No Android suite for documentation-only work. Recheck only failures or content invalidated by later edits.
