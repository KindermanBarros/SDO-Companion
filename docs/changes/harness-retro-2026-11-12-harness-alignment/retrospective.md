# Align the SDO harness and retire setup inputs

Date: 2026-11-12 19:19 America/La_Paz.

## Incident and authorization

The owner requested final setup-file cleanup and updates to .agents workflows, agents and gates, then confirmed a preference for automatic commits.
The configured .docs baseline and the older generic .agents instructions disagreed about source layout, skill invocation, git behavior and setup entry points.
The initial working tree already lacked harness/scripts and harness/templates; those deletions were preserved.

## Classification and invariant

Primary classification: HARNESS_ENVIRONMENT.
Instruction integrity must not imply successful application verification. Missing required runtime/tooling remains non-passing.
Automatic commits follow the owner's recorded preference after applicable validation; branch creation, push, PR, merge and release still require a request.

## Changes

- Updated all 10 workflow entry points, 4 agent profiles and 3 gate checklists.
- Added .agents/gates/project-contract.md as the shared integration authority.
- Retained the original mechanical gate inventory in linked Contracts/source_and_runtime documents within the 120-line limit.
- Corrected two skill entry references so baseline refresh no longer requires the retired setup prompt.
- Updated root git policy and .docs git/validation/context/task/skills records.
- Added a native PowerShell structural gate and isolated negative regression fixtures.
- Removed setup_prompt.md, the superseded setup task and its temporary survey after preserving useful facts in permanent context/history.
- Preserved product requirements, backlog, onboarding approvals, historical evidence, application code, builds and pre-existing deleted tooling.

## Verification

- Before cleanup: structural validator exited 1 with Stale setup input: setup_prompt.md.
- After cleanup: powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/validate-harness.ps1 exited 0.
- Regression suite: powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .agents/gates/tests/validate-harness-tests.ps1 exited 0; 9 cases.
- Negative cases: broken link, broken anchor, placeholder, line limit, import, sealed banner, stale setup input, missing baseline.
- git diff --check scoped to the maintained harness paths passed.
- The test suite verified expected diagnostics and removed its temporary fixture directory in finally.
- No Android build/runtime test was executed: this change modifies instructions/documentation and a standalone structural gate, not app compilation.
- Legacy harness contract tests could not run because their scripts were absent before this task.
- Product behavior: Confirmed unchanged. No feature tracker state or capability requirements changed.

## Remaining risk and handoff

The structural gate certifies its declared instruction checks only. It does not resolve prior Android source, style, coverage, platform or onboarding blockers.
The legacy scripts and rubric templates remain prerequisites for workflows that require them.
Historical references to the retired setup prompt remain as audit history, not active setup dependencies.
Task state is InDevReview. Developer acceptance and any application/tooling repair remain separate.

Skills applied: harness-router, context-management and harness-retrospective, read through local SKILL.md files.
