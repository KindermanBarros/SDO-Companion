# Shared campaign journal - InProgress - 3

- Date time: 2026-11-14 01:36 America/La_Paz
- Task objective: Deliver BACKLOG-001 / CAMPAIGN-PLAN-001.
- Owner authorization: Start journal; accept release and optimization deliveries.
- Open questions: None; owner approved specification with "Do it" on 2026-11-14.
- Shared dependencies and references: [Change record](../../docs/changes/2026-11-14-shared-campaign-journal.md), CAMPAIGN-PLAN-001 and SYNC-PLAN-001 for journal operations.
- Sub-tasks:
    - 1 Refine requirements and approve specification - Done
        - Owner confirmed shared text entries; members create, authors edit/delete their own, GM moderates all.
        - Source observations: GM is campaign owner; HISTORIAN member enum does not confer GM authority.
        - Definition of done: explicit specification approval.
    - 2 Define UI contract and implementation slices - Done
        - Cover list/editor, permissions, offline, conflicts and recovery using existing smartphone components.
        - Dependencies: approved specification and visual direction where required.
        - Owner rejected the generated designs for styleguide noncompliance on 2026-11-14; they are not approved references.
    - 3 Implement and verify approved slices - ToDo
        - Expected files: campaign domain/data, Firebase rules/tests, presentation and focused tests.
        - Technology and module changes: None authorized; reuse Firebase/Android stack.
        - Definition of done: secure access, online-only writes, cross-session updates and runtime evidence.
- Checks selected for documentation: harness links/anchors/line limits and staged diff; Android checks selected after approved implementation scope.
- Owner correction: add the styleguide to the Pen creation skill; bounded files: pen-design-editor/SKILL.md and affected workflow records. Inspect guide/component gaps, patch the skill, validate skill/harness integrity, commit for review.
- Skill correction & UI handoff: [Change record](../../docs/changes/2026-11-14-shared-campaign-journal.md) (consolidated spec, UI contract, 52 exported references, design review and implementation/test plan). Owner approved UI and requested slightly more color in Standard; applied semantic blue borders/cyan indicators and promoted journal.pen. No app code changed. BACKLOG-001 implementation remains pending.

- Color refinement - InDevReview: Standard information panels use secondary blue; indicators cyan. Existing layout and Cybergrunge preserved. Next action: implement the approved journal plan; no further visual approval loop for this bounded adjustment.
