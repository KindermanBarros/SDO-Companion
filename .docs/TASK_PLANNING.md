# TASK_PLANNING
Plan before editing; read-only inspection needs no task file. Resume an existing task instead of creating another status system.

## Proportional planning
- Select the requested item from the [living backlog](../docs/product/product.md#backlog-de-produto); link its existing ID or record the direct owner request.
- For bounded fixes, record goal, affected files, dependencies, 3-7 executable steps and checks in the task state; no duplicate spec/approval loop.
- Significant features/architecture/data changes use the owning workflow and approved contracts.
- Record real dependencies and technology/module changes before introducing them; use None when absent.
- Preserve unrelated work and reuse approvals for unchanged scope. Ask only about unresolved product decisions.
- Task states and approved contracts are durable documentation, never temporary files.
- Durable complex workspaces live in docs/changes/YYYY-MM-DD-feature-name; ad-hoc records remain in docs/current.
- Throwaway files belong only in .scratch, which must be absent before every commit.

## States
| State | Meaning |
|---|---|
| ToDo | Planned |
| InProgress - N | Sub-task N executing |
| InAgentValidation | Selected checks running |
| InDevReview | Agent handoff; append history and reconcile backlog |
| Done | Developer-approved only |

Only developer approval moves work to Done. Then remove its task-state file and index row; retain durable decisions/evidence and backlog disposition.
Add new findings to the backlog without silently expanding the active task. Completed/accepted backlog entries remain as concise traceability, not silently deleted requests.

## Template
```markdown
# Task title - State
- Date time: YYYY-MM-DD HH:MM America/La_Paz
- Task objective: Concrete completion objective.
- Open questions: None | Pending decision
- Shared dependencies and references: None | Existing contract/dependency
- Sub-tasks:
    - 1 Executable sub-task - State
        - Steps, files and relevant verification.
        - Dependencies: None | Shared reference
        - Technology and module changes: None | Approved delta
        - Definition of done:
            - [ ] Observable result
```

## Ongoing tasks
| Task | File | Started |
|---|---|---|
| Optimize workflows and tools | [Task state](OngoingTasks/lean_workflows_and_tools_task_state.md) | 2026-11-14 |
| Publish v0.51.1 | [Task state](OngoingTasks/release_0_51_1_task_state.md) | 2026-11-13 |
| Reconcile pending worktrees | [Task state](OngoingTasks/worktree_reconciliation_task_state.md) | 2026-11-13 |

Accepted records: [2026-11-13](CompletedTasks/accepted_2026_11_13.md).
