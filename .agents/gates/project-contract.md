# SDO harness contract
Root [AGENTS](../../AGENTS.md) owns workflow, temporary-file, backlog and token policies. Read it once; do not duplicate its rules in stage reports.
Use the [execution checklist](../rules/working-economically.md) when optimizing context or verification.

## Baseline
- Single :app Android module; API 30 minimum, 37 compile/target; confirmed device is smartphone.
- Manual wiring, repository interfaces, multiple state flows and custom saveable navigation are the observed architecture.
- Follow .docs architecture/standards. Generic harness patterns do not authorize migrations.

## One execution record
| Record | Authority |
|---|---|
| Backlog and capability index | docs/product/product.md |
| Living behavior | docs/product/requirements/ |
| Task state | .docs/OngoingTasks/ |
| Ad-hoc contracts/summary | docs/current/ |
| Durable complex contracts/decisions | docs/changes/YYYY-MM-DD-feature-name/ |
| Final evidence | Linked docs/current or docs/changes record |
| Temporary QA/logs/downloads/PR drafts | .scratch/; delete before every commit |

Backlog is live: select requested work, add findings/requests and reconcile status. Adding an item does not approve implementation.
Registered worktrees in .worktrees are managed Git checkouts, not disposable scratch outputs. Do not put checkouts/contracts in .scratch or delete dirty work.

## Proportional stages
- A bounded fix can use one owner and one short task plan; keep complex contracts only when their scope requires them.
- Existing explicit owner authorization covers routine implementation, fixes and publication in that scope; do not ask again at every stage.
- Delegate only for meaningful parallel work or justified independent review; role names are responsibilities, not mandatory agent calls.
- Review the changed risk, not the entire repository. No separate report per stage for trivial work.

## Verification and handoff
Use [VALIDATION](../../.docs/VALIDATION.md) and [CI checks](ci-checks.md). Select once, reuse matching evidence, rerun only invalidated checks.
Owner build-only/manual-runtime preferences override default local execution, not truthfulness or acceptance.
Missing devices, failed quality/coverage or absent visual infrastructure remain explicit limitations; never claim a fake/JVM check proves a platform journey.
Legacy gates backed by absent scripts remain retired. Do not restore them or introduce dependencies to satisfy an unrelated task.
Only authenticated Firebase-configured APKs with verified registered certificates may be delivered.
At handoff update affected docs/backlog, append history and enter InDevReview. Remove .scratch, run the cleanup hook and scoped staged diff check, then commit only task files with sign-off.
Push/PR/merge/release require existing user authorization; only the developer approves Done.
