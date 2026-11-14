# Workflow cleanup and lean delivery - Done - 3

- Date time: 2026-11-13 20:16 America/La_Paz
- Task objective: Remove disposable artifacts, reserve .scratch for temporary work, maintain a living backlog, reduce repeated validation/output, and register the existing senior persona as an agent.
- Open questions: None; owner explicitly authorized cleanup, workflow/documentation changes and commit/push.
- Shared dependencies and references: D1. Root AGENTS, existing .docs policies, native harness validator and role profiles. D2. Registered PR worktree must be preserved outside disposable output. No application dependency changes.
- Sub-tasks:
    - 1 Inventory and preserve durable work - Done
        - Preserve dirty root code/backlog/attachments and committed PR work; relocate checkout; archive the tracked scratch handoff; classify disposable logs/downloads/intermediate designs.
        - Dependencies: See D1 and D2
        - Technology and module changes: None
        - Definition of done:
            - [x] Disposable paths are explicit and confined to the repository.
    - 2 Consolidate workflow, backlog and agent - Done
        - Update central policies and conflicting callers; add pre-commit scratch enforcement; register laziest_senior_developer without pinning a model; reconcile existing backlog records.
        - Dependencies: See D1
        - Technology and module changes: None; repository-local agent/hook configuration only.
        - Definition of done:
            - [x] .scratch contains no durable contracts and cannot survive a commit.
            - [x] Backlog participates in selection/reconciliation; checks and output are proportional.
    - 3 Clean, validate and publish - Done
        - Remove run-owned disposable files and stopped local tools; run one scoped structural/regression pass; delete scratch before the signed-off commit; push the existing PR.
        - Dependencies: Steps 1 and 2
        - Technology and module changes: None
        - Definition of done:
            - [x] Actual checks, removals and limitations recorded; task enters Done.

- Evidence: [Retrospective](../../docs/changes/harness-retro-2026-11-13-lean-workflow/retrospective.md).

Owner explicitly approved closure of all five tasks, including UI, on 2026-11-13. Historical check limitations remain unchanged.
