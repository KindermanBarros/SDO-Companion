---
name: harness-router
description: Resume or route SDO work using the requested outcome, live backlog and recorded authorization.
---
# Harness Router
Root AGENTS and the [Project contract](../../gates/project-contract.md) take precedence over generic stage instructions.

## Resume before rediscovery
1. Read root policy once and the requested living backlog/capability entries.
2. Read the indexed task state and its active summary/approved contract, not every historical version.
3. Continue the first incomplete relevant step. Folder location or artifact existence is not approval.
4. Preserve unrelated work; ask only when scope/product decisions or authorization remain unresolved.

## Select one route
| Intent | Entry |
|---|---|
| Defect | workflows/bug-fixing.md |
| Bounded feature/enhancement | workflows/feature-delivery.md |
| UI change | workflows/create-ui-and-verify.md |
| Complex ordered delivery | workflows/harness-planning.md |
| Next approved complex slice | workflows/harness-generator.md |
| Evaluator findings on complex work | workflows/harness-fix.md |
| Independent review | workflows/feature-review.md or harness-evaluation.md |
| Post-merge acceptance | workflows/feature-acceptance.md |
| Temporary mobile exploration | skills/exploratory-qa/SKILL.md |
| Harness workflow/gate/evidence repair | skills/harness-retrospective/SKILL.md |
| Docs/backlog only | skills/documentation-and-adrs/SKILL.md |
| Existing static checks | skills/android-code-quality-checks/SKILL.md |

Resolve these paths from .agents/. Read only the selected current-stage skill and triggered rules.
For bounded work use one owner and a short task plan. Delegate only when requested or when justified by distinct work/review risk.
Complex work still uses its approved feature list/sprint contract; do not duplicate it with another plan.

## Durable versus temporary
Backlog: docs/product/product.md. Requirements: docs/product/requirements/. Task state: .docs/OngoingTasks/.
Ad-hoc contracts: docs/current/. Complex contracts/evidence: docs/changes/YYYY-MM-DD-feature-name/.
Temporary QA/logs/downloads/drafts: .scratch only. Promote final evidence and remove the entire scratch folder before every commit.

## Progress without loops
Reuse existing explicit owner approvals for unchanged scope, including CI/manual-test preferences.
A stage handoff does not require another approval, report, agent or test run.
Select checks once using .docs/VALIDATION.md; reuse matching evidence, rerun only invalidated checks.
Do not restore retired legacy gates, silently waive security/data-loss risks or report unexecuted runtime evidence as passing.
Read-only review does not authorize fixing unrelated findings: add them to backlog.
At handoff give result, evidence/limitation and one next action; update the same task and backlog records.
