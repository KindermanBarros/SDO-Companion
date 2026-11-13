# TASK_PLANNING

Plan every task that modifies files before you edit anything. Questions and read-only analysis need no plan.

- Create `OngoingTasks/task_name_task_state.md`, with the task name in `snake_case`, and add it to [§ Ongoing tasks](#ongoing-tasks). Create the folder if it doesn't exist.
- If a task-state file for the same task already exists, resume from it instead of creating a new one.
- Keep the states current while you work, so an interrupted task can resume from the file.
- Edit only § Ongoing tasks in this file. Everything else changes only when the user explicitly asks.

## Decomposition

1. **Goal:** state it explicitly and concretely. A vague goal ("improve the product") decomposes poorly. If it's vague, sharpen it by asking; don't guess.
2. **Top-level sub-tasks:** identify the 3–7 major pieces of work.
3. **Granularity:** for each sub-task, ask: "Could I execute this directly, or does it still hide several steps?" Recurse until every leaf can be executed directly.
4. **Dependencies:** identify which sub-tasks must finish before others can start.
5. **Order:** order the sub-tasks, or mark them as parallelizable. This is the plan.
6. **Assign:** assign every leaf task to a sub-task. Each sub-task is one execution step.

## Planning standards

- Verify that files, contracts, modules and checks exist before you name them. Don't create structure only to complete a plan.
- **Dependencies:** verify the task's dependencies before you plan the implementation. Record existing and new dependencies in the sub-task's `Dependencies`, with their purpose, impact, decision status and source. Record a dependency that spans several sub-tasks once, in `Shared dependencies and references`, and link it from each affected sub-task. Write `None` when no dependency applies.
- **Technology and module changes:** list the technologies and modules that are added, changed or removed, with their purpose, impact and decision status. Record each change in its responsible sub-task, and link it from the other affected sub-tasks. If the change is also a dependency, reference its dependency entry instead of repeating the details.
- The responsible sub-task also covers the documentation updates (confirmed technology selections go into [TECH_STACK](TECH_STACK.md)) and the verification.
- Pending decisions go into `Open questions`, and they block every sub-task that depends on them.
- If you discover a new dependency, technology change or module change during implementation, update the plan before you introduce it.
- Use the template headings and terminology unchanged. Preserve unrelated work.

## States

| State | Meaning |
|---|---|
| `ToDo` | Planned, not started |
| `InProgress` | Being implemented. On the task, write `InProgress - sub-task number` with the number of the sub-task in progress |
| `InAgentValidation` | You are running the checks in [VALIDATION](VALIDATION.md) |
| `InDevReview` | Handed to the developer for review. Append the history record ([HISTORY](HISTORY.md)) when entering this state |
| `Done` | Validated and approved by the developer |

- Only the developer's approval moves a task or sub-task to `Done`. If the review asks for changes, move the affected sub-tasks back to `InProgress`.
- When the **task** is `Done`, delete its task-state file and remove its row from § Ongoing tasks.

## Template

```markdown
# Task title - State

- Date time: YYYY-MM-DD HH:MM America/La_Paz
- Task objective: Concrete completion objective.
- Open questions: None | Question and recorded answer
- Shared dependencies and references: None | D1. Dependency name: purpose; impact; status; source
- Sub-tasks:
    - 1 Sub-task title - State
        - Describe the executable steps.
        - Dependencies: None | Dependency name: purpose; impact; status; source | See D1
        - Technology and module changes: None | Added/Changed/Removed technology: purpose; impact; status
        - Definition of done:
            - [ ] Verifiable condition
```

## Ongoing tasks

| Task | File | Started |
|---|---|---|
| Align harness and remove setup inputs | [Task state](OngoingTasks/harness_alignment_task_state.md) | 2026-11-12 |
| Remove absent-script gates | [Task state](OngoingTasks/remove_missing_gates_task_state.md) | 2026-11-12 |
| Authenticated test APKs | [Task state](OngoingTasks/authenticated_builds_task_state.md) | 2026-11-12 |
