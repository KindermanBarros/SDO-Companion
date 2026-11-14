---
name: exploratory-qa
description: Execute mobile QA plans or explore feature risks with agent-device.
---
# Exploratory QA

Extends [dogfood](../dogfood/SKILL.md) for ephemeral device exploration and [Feature Acceptance](../../workflows/feature-acceptance.md) execution. Follow the shared [artifact layout](references/artifact-layout.md).

## Target and scenario
Keep all ephemeral scenarios, captures, logs, and reports in `.scratch/qa/<run-id>/` and delete `.scratch` before commit after promoting any durable summary. Never create permanent `androidTest` classes or CI jobs for an exploratory run.
Declare `"execution": {"mode": "ephemeral", "driver": "agent-device", "regression_targets": [], "ci_required": false}` in `.scratch/qa/<run-id>/scenario.json` (see [scenario sources](references/scenario-sources.md)), and map in-scope vs excluded entry points before starting UI actions.

## Modes
1. **Approved-plan mode**: Verify approval hashes and installed APK identity ([plan review](../../workflows/feature-acceptance.md#1-plan--test-design)). Execute cases serially via `agent-device`, asserting UI, state, and backend/mock oracles (`PASS`, `FAIL`, `INCONCLUSIVE`, `SKIPPED`). If a real-server flow is blocked, return to [mock-flow replanning](../../workflows/feature-acceptance.md#1-plan--test-design).
2. **Acceptance-support / standalone mode**: Explore uncovered risk targets within a time/action budget. Distinguish `confirmed defect`, `suspected issue`, and `unverified hypothesis`.

## Internal state & analytics
When HarnessLens is in scope, invoke [harnesslens](../harnesslens/SKILL.md) and follow [references/harnesslens.md](references/harnesslens.md). Standalone analytics captures may use [analytics-event-validator](../analytics-event-validator/SKILL.md).

## Network mode
- **No proxy**: UI + state/analytics evidence only.
- **Capture / Mock**: Use [api-mocking](../api-mocking/SKILL.md#run) or [references/proxy.md](references/proxy.md). Link mocked interactions in `expectations.mock_coverage` and verify hits before cleanup.

## Dogfood report reconciliation
Reconcile manual observations from `dogfood-report.md` into `observations.json.findings` and set `dogfood_report_reviewed: true` before running `python3 .agents/skills/exploratory-qa/scripts/report.py --run-dir .scratch/qa/<run-id> --scenario .scratch/qa/<run-id>/scenario.json`.

## Cleanup & handoff
Always restore device/proxy/mock state in a finally path and record cleanup receipts. Pass confirmed defects (`report.json` path + finding ID) to [Bug Fixing](../../workflows/bug-fixing.md) without repeating the QA session.
