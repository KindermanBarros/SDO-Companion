# Living product documentation
The [backlog](../../docs/product/product.md#product-backlog) is part of daily workflow, not an archive.

1. At orientation read the requested backlog row and affected capability requirements; use existing IDs.
2. Record newly requested work, regressions and useful discoveries in that same backlog, with source, status and unresolved decisions.
3. Select only owner-authorized scope; backlog presence does not grant implementation or priority.
4. Link the task state and approved contract. Reuse the same record on continuation.
5. At handoff reconcile implemented, awaiting-review, blocked and deferred scope; only owner approval closes accepted work.
6. Preserve requests/decisions and their history; do not silently delete undelivered backlog items or claim partial UI work delivers broader capabilities.

Product index/lifecycle: docs/product/product.md. Detailed living requirements: docs/product/requirements/.
Durable dated contracts/decisions/evidence: record in `docs/MEMORY.md` (optional binary assets under `docs/changes/assets/`). Never create separate `docs/changes/*.md` files. Ad-hoc spec/plan/summary: `docs/current/`.
Temporary investigation/QA output: .scratch/ only, always removed before commit after promoting necessary final evidence.
Never put approvals, task states, backlog or durable contracts in scratch.

Update existing capability claims in place; distinguish proposed intent, implemented source and verified acceptance.
For meaningful behavior changes, link the affected requirement IDs and record the concise disposition in the active summary.
For documentation-only workflow maintenance, record product behavior unchanged; do not generate fake product specifications.
