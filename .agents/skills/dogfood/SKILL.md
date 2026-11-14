---
name: dogfood
description: Explore and test a mobile app on Android/iOS with agent-device to find runtime bugs and UX issues.
allowed-tools: Bash(agent-device:*), Bash(npx agent-device:*)
---

# Skill — Dogfood

## Execute
1. Verify `agent-device --version` (`>= 0.14.0` required; stop and ask user if missing/outdated — never auto-install `@latest`).
2. Run `agent-device help dogfood` for CLI report and evidence conventions.
3. Loop: open app -> `snapshot -i` + screenshot -> inventory in-scope entry points -> exercise routes -> capture runtime evidence per issue -> close. Record out-of-scope routes with reasons.

## Report Handoff
Follow `agent-device help dogfood` for standalone runs, or [exploratory-qa report reconciliation](../exploratory-qa/SKILL.md#dogfood-report-reconciliation) when invoked by an owning QA workflow.
