# Align harness and remove setup inputs - Done

- Date time: 2026-11-12 19:15 America/La_Paz
- Task objective: Align .agents workflows, role profiles and gates with the populated SDO harness, validate the result, then remove disposable setup inputs.
- Open questions: None. Owner requested cleanup/instruction updates and confirmed preference for automatic commits.
- Shared dependencies and references: D1. Existing AGENTS.md and .docs: confirmed policy and source baseline. D2. Missing harness scripts/templates: existing prerequisite gaps; mandatory application gates remain blocked.
- Sub-tasks:
    - 1 Diagnose and plan - Done
        - Read affected instructions, identify absent prerequisites, automatic git actions and stale setup routes. Preserve unrelated working-tree changes.
        - Dependencies: See D1 and D2
        - Technology and module changes: None
        - Definition of done:
            - [x] Invariant: documentation readiness does not imply application-gate success; automatic commits follow owner preference and validation.
    - 2 Align instructions and gates - Done
        - Update workflows, agents and gates; connect local policy and document available versus missing verification commands. Add a native structural gate with negative regression cases.
        - Dependencies: See D1 and D2
        - Technology and module changes: None
        - Definition of done:
            - [x] Instructions agree with root policy; 1 positive and 8 negative contract cases pass.
    - 3 Validate, clean up and hand off - Done
        - Remove the setup prompt and superseded temporary task/survey only after preserving relevant facts in durable harness records. Validate links, limits, scope and append history.
        - Dependencies: See D1
        - Technology and module changes: None
        - Definition of done:
            - [x] Structural checks and scoped whitespace checks pass; prior application blockers remain explicit.

[Maintenance record](../../docs/changes/harness-retro-2026-11-12-harness-alignment.md)

Owner explicitly approved closure of all five tasks, including UI, on 2026-11-13. Historical check limitations remain unchanged.
