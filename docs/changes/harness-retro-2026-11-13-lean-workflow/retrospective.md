# Lean workflow retrospective

Classification: WORKFLOW_GAP. Status: Done; explicit owner acceptance on 2026-11-13. Timestamp: 2026-11-13 20:18 America/La_Paz.

The owner authorized temporary-file cleanup, living backlog, fewer repeated checks, concise documentation and a senior agent configuration.

## Invariants and changes

- All agent throwaway output belongs in .scratch; the entire directory must be absent before every commit. A local pre-commit hook rejects both existing scratch and staged temporary paths.
- Durable contracts belong in docs/changes. The historical PR136 handoff was archived there; the active worktree moved from disposable tooling to .worktrees/pr-compact-ui.
- Backlog selection, new requests/findings and review-state reconciliation are part of delivery. Existing six product requests remain proposed; no priorities or broad feature acceptance were invented.
- Central instructions and stage workflows now select checks by changed boundary, reuse valid evidence and report once. Existing senior persona is also registered as a repository agent, inheriting the session model.
- Removed 11 tracked raw logs, four intermediate Pen files, one intermediate screenshot, downloaded local emulator/SDK output and repository build caches. Preserved approved design, selected screenshots, durable summaries, user attachments, installed SDK and unrelated dirty source.

## Verification

Harness integrity passed (43 documents). Contract regression passed 11 cases, scratch guard passed four cases, runner shell control passed four cases. git diff --check passed.
Python QA helper tests were not run: python is unavailable on PATH. No Android tests/build or runtime acceptance was claimed for this workflow-only change. New agent configuration is discovered in a fresh session; no agent execution was required.
Only invalidated checks were repeated after corrections. Owner accepted the delivery on 2026-11-13.
