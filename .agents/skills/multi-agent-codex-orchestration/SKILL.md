---
name: multi-agent-codex-orchestration
description: Orchestrate parallel Gemini swarm workers alongside a resumed Codex CLI session with isolated workspaces, schema ownership, non-blocking monitoring, and deterministic git integration.
---
# Multi-Agent Codex + Gemini Orchestration

Use this skill when continuing an existing Codex CLI session in parallel with Gemini swarm workers on the same repository.

## 1. Session Discovery & Non-Interactive Resume
- Inspect `~/.codex/history.jsonl` and `~/.codex/sessions/YYYY/MM/DD/rollout-*.jsonl` to locate the target `session_id`, recorded `cwd`, and last turn status (`turn_aborted` vs `item_completed`).
- Never invoke interactive `codex resume` in a non-TTY tool call; always use:
  `codex exec resume --all --dangerously-bypass-approvals-and-sandbox [SESSION_ID] "[PROMPT]"`
- Always include `--all` when the session originated outside the repository root (such as `$HOME`), and state the exact repository root path inside `[PROMPT]`.

## 2. Workspace & Schema Isolation
- Keep the warm Codex session in the primary working copy (`workspace: "inherit"` via a lightweight shepherd worker) and spawn parallel Gemini workers with `workspace: "share"` (or `"branch"`) so each gets an isolated git worktree and Gradle build state.
- Partition file ownership explicitly in every worker brief (`Owns:` and `Must NOT touch:`).
- When Room/SQLite schema changes overlap, assign `AppDatabase` version bumps (`MIGRATION_N_N+1`) to only one worker and instruct the other to reuse existing map/JSON columns (such as `fieldUpdatedAt`) or pre-assign sequential migration numbers.
- Preserve unrelated staged or unstaged changes (such as `D firebase-firestore-audit.md`) by excluding them from worker commits and temporarily unstaging them before `git cherry-pick` when needed.

## 3. Non-Blocking Shepherd & Monitor Behavior
- Have the shepherd launch `codex exec resume` with `WaitMsBeforeAsync: 10000` and let it run in the background without polling in a loop.
- While parked waiting on a background task, `swarm status` reports the shepherd as `waiting`, which can trigger `swarm_monitor` `[stall]` checks. When nudged, inspect the background task status or `rollout-*.jsonl` tail and reply with live progress (for example `last progress Xs ago`) instead of failing.

## 4. Deterministic Integration & Gates
- Land or `git cherry-pick --no-commit` all `share` workspace commits into the primary workspace BEFORE calling `swarm{action: "kill"}` (which deletes the `share` worktree).
- Reconcile overlapping domain models, sync conflict groups, UI screens, `docs/product/product.md`, and `docs/MEMORY.md` so both workstreams are preserved.
- Validate with `.\gradlew.bat :app:assembleDebug` (using Android Studio JBR if `JAVA_HOME` is unset), `.agents/gates/validate-harness.ps1`, `tools/pre-commit-cleanup.ps1`, and `git diff --check`, then create a scoped signed-off commit (`git commit -s`).
