# Harness Retrospective — Workflow, Skill, Rule & Tool Token Optimization

- **Date**: 2026-11-14 00:26 America/La_Paz
- **Branch**: `feature/lean-workflows-and-tools` (based on `main` `6d85b83`, including all SDO Companion improvements)
- **Task state**: [.docs/OngoingTasks/lean_workflows_and_tools_task_state.md](../../../.docs/OngoingTasks/lean_workflows_and_tools_task_state.md)

## Problem Statement

1. **Always-loaded & conditional rules bloat**: `.agents/rules/testing-strategy.md` (injected into `<user_rules>` on every turn) and the 10 conditional `.agents/rules/*.md` files contained ~1,440 lines of multi-screen tutorials, non-SDO code blocks (`Hilt`, `Retrofit`, `LocalAppColors`, `NoteDetailScreen`, `RoborazziTest`, `BaseViewModelTest`), and repeated boilerplate.
2. **Oversized workflows, skills, references, and templates**: `.agents/workflows/*.md`, `.agents/skills/*/SKILL.md`, skill reference/example files, and `harness/templates/*` consumed over 7,000 lines across 70+ files, contained 6 broken relative links/anchors, used invalid frontmatter (`summary:` instead of `description:` in `harnesslens/SKILL.md`), and still referenced retired `harness/scripts/*.sh` commands in `harness/templates/`.
3. **Validator & profile gaps**: `.agents/gates/validate-harness.ps1` did not validate `.agents/rules` or `.agents/skills` for line limits, placeholders, relative links, or heading anchors, nor did it scan `harness/templates` for retired script references. `harness-project.json` used unqualified `ktlintCheck` and `detekt` commands instead of `:app:ktlintCheck` and `:app:detekt`, and `.agents/workflows/.DS_Store` was tracked in git.

## Root Cause

Earlier harness imports copied generic multi-platform/Hilt/Retrofit/Roborazzi instruction packs and templates verbatim without pruning tutorial examples or extending `validate-harness.ps1` to enforce the `<= 120`-line, link/anchor, and retired-script invariants on `.agents/rules`, `.agents/skills`, and `harness/templates`.

## Changes Applied

- **Compacted 11 `.agents/rules/*.md` files** (from ~1,440 lines to ~200 lines; `testing-strategy.md` reduced from 116 lines to 25 lines), preserving required anchors (`#evidence-invariants`, `#platform-bound-evidence`) and aligning rules with SDO Companion's actual architecture (`Manual DI`, `Room`, `DataStore`, `Firebase`, `AppSurface`).
- **Compacted 6 `.agents/workflows/*.md` files** to `13–21` lines each while preserving required anchors (`#1-plan--test-design`).
- **Compacted 36 `.agents/skills/*/SKILL.md` files** and 2 oversized reference/example files (`api-mocking/references/usage.md`, `test-design/examples/node-sharing.md`), fixed `harnesslens/SKILL.md` YAML frontmatter (`description:`), and repaired all 7 broken relative links/anchors (`implementation-plan`, `api-mocking`, `exploratory-qa`, `test-design`).
- **Compacted 12 `harness/templates/*` files** (`test-plan-template.md`, `sprint-contract-template.md`, `code-review-template.md`, `implementation-plan-template.md`, `feature-spec-template.md`, `clean-state-checklist-template.md`, `evaluator-rubric-template.md`, `test-review-template.md`, `summary-template.md`, `platform-capability-matrix-template.md`, `feature_list_template.json`, `project-profile.example.json`) and removed all retired `harness/scripts/` references.
- **Extended `.agents/gates/validate-harness.ps1`** to validate `.agents/rules` and `.agents/skills` (now covering **114 harness documents** for `<= 120` lines, placeholders, relative links, and heading anchors) and to scan `harness/templates` for retired `harness/scripts/` references.
- **Aligned `harness-project.json`** (`:app:ktlintCheck`, `:app:detekt`) and removed tracked `.agents/workflows/.DS_Store`.

## Verification Evidence

| Check | Command | Result |
|---|---|---|
| Structural & link/anchor gate | `powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/validate-harness.ps1` | `PASS: 114 harness documents; links, anchors, limits, imports, banners, placeholders and cleanup.` |
| Gate negative/positive regression suite | `powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/tests/validate-harness-tests.ps1` | `PASS: 11 contract cases (1 positive, 10 negative).` |
| Pre-commit `.scratch` cleanup suite | `powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/tests/scratch-cleanup-tests.ps1` | `PASS: 4 pre-commit scratch cleanup cases.` |
| Net footprint reduction | `git diff --shortstat main` | `~6,250 net lines removed (~89% reduction across touched harness files)` |
