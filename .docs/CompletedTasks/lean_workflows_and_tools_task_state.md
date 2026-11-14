# Optimize workflows, skills, rules and tools for low token usage - Done

- Date time: 2026-11-14 00:26 America/La_Paz
- Task objective: Create a feature branch containing the SDO Companion improvements and compact workflows, rules, skills, templates and harness validator tools to drastically reduce token consumption while fixing broken links and enforcing size limits.
- Open questions: None; owner explicitly requested creating the feature branch and optimizing workflows and tools for lower token usage.
- Shared dependencies and references: D1. Root AGENTS.md, .docs baseline and .agents/gates/project-contract.md. No application dependency changes. Retrospective: [retrospective.md](../../docs/changes/harness-retro-2026-11-14-token-optimization/retrospective.md).
- Sub-tasks:
    - 1 Create feature branch and compact workflows/rules - Done
        - Created `feature/lean-workflows-and-tools` from `main` (`6d85b83`); compacted auto-loaded `testing-strategy.md` (116 -> 25 lines), all 11 `.agents/rules/*.md` files, and 6 `.agents/workflows/*.md` files (`<= 29` lines each).
        - Dependencies: See D1
        - Technology and module changes: None
        - Definition of done:
            - [x] Feature branch checked out on top of Companion improvements; workflows and rules compacted <=60 lines.
    - 2 Compact skills, references, templates and tools - Done
        - Compacted 36 `.agents/skills/*/SKILL.md` files, 2 reference/example files, and 12 `harness/templates/*` files; fixed 7 broken relative links/anchors and `harnesslens` YAML frontmatter; removed retired `harness/scripts/` references from templates; aligned `harness-project.json` (`:app:ktlintCheck`, `:app:detekt`); removed tracked `.agents/workflows/.DS_Store`; extended `validate-harness.ps1` to validate `.agents/rules`, `.agents/skills`, and `harness/templates`.
        - Dependencies: Step 1
        - Technology and module changes: None
        - Definition of done:
            - [x] All skills, rules and workflows pass strict link, anchor and line-limit validation with >60% token reduction (~89% reduction across touched files).
    - 3 Validate, record evidence and commit - Done
        - Ran `validate-harness.ps1` (114 docs PASS), `validate-harness-tests.ps1` (11/11 PASS), `scratch-cleanup-tests.ps1` (4/4 PASS), and `git diff --cached --check`; recorded [retrospective.md](../../docs/changes/harness-retro-2026-11-14-token-optimization/retrospective.md), updated backlog/history, verified `.scratch` is absent, and created a signed-off commit.
        - Dependencies: Step 2
        - Technology and module changes: None
        - Definition of done:
            - [x] Structural and regression checks pass; task enters Done with signed-off commit on `feature/lean-workflows-and-tools`.

Owner explicitly approved this delivery on 2026-11-14. Existing evidence and limitations remain unchanged.
