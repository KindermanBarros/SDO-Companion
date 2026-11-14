# Agent instruction ownership

| Layer | Owns | Does not own |
|---|---|---|
| [Root AGENTS](../AGENTS.md) | Project-wide invariants, authorization and handoff | Duplicated stage manuals |
| [Documentation](../docs/README.md) | Engineering/product/design facts and policies | Runtime-specific agent registration |
| `.agents/agents/` | Role-specific responsibilities | Model selection or copies of project-wide policy |
| `.agents/workflows/` | Task routing and stage order | Another architecture/design authority |
| `.agents/skills/` | Procedures for the selected capability | Another copy of shared rules |
| `.agents/rules/` | Narrow triggered implementation rules and links to canonical policy | Global workflow restatements |
| `.agents/gates/` | Executable structural checks and current CI/review catalog | Retired script gates |
| `.codex/agents/` | Codex model/description metadata and links to the matching role | Behavioral policy or application work files |

Read the relevant layer only. A role name does not require spawning an agent or repeating a stage.
The canonical role is `implementer`; historical references to Generator refer to that responsibility. The `harness-generator` workflow name describes slice generation and remains unchanged.
The persona under `docs/engineering/Personas/` adds decision style; roles and adapters link to it instead of copying validation, cleanup and lifecycle policy.
User-level `~/.agents/skills`, `~/.codex/skills` and `~/.codex/worktrees` are external installations/checkouts, not additional repository ownership layers.
