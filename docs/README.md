# Documentation

This is the single documentation root. Start with the topic you need; do not load every guide.

| Topic | Canonical owner |
|---|---|
| Project-wide agent invariants | [AGENTS](../AGENTS.md) |
| Product scope and backlog | [Product](product/product.md) and its linked requirements |
| Architecture policy | [Architecture](engineering/ARCHITECTURE.md) |
| Package ownership and current flows | [Project context](engineering/PROJECT_CONTEXT.md) |
| Observed architecture evidence | [Source map](architecture/architecture-map.md); not policy |
| Code and resource conventions | [Code standards](engineering/CODE_STANDARDS.md) |
| Dependencies and build configuration | [Tech stack](engineering/TECH_STACK.md) |
| Visual identity and interaction | [Design system](design/design_system.md) |
| Task planning and history | [Planning](engineering/TASK_PLANNING.md) and [Memory](MEMORY.md) |
| Verification selection | [Validation](engineering/VALIDATION.md) |
| Git operations | [Git standards](engineering/GIT_STANDARDS.md) |
| Agent roles and adapters | [Agent ownership](../.agents/README.md) |

## Ownership

`engineering/` holds governing engineering policy; `architecture/` holds source observations.
`contracts/` holds existing data/domain contracts; the [Firebase boundary](engineering/Architecture/firebase_contract.md) indexes SDK ownership.
`design/` owns shared and mode-specific design rules. Task-specific UI contracts and approved Pen references remain under `current/` or `changes/assets/`.
`current/` contains scoped contracts/evidence, not another policy hierarchy. Completed task summaries belong in Memory.
An observed implementation, historical plan or successful design export does not override policy or establish runtime acceptance.
