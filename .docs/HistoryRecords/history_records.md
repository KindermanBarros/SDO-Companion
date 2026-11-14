# History records — SDO Companion

Append-only audit log of agent modifications, oldest first. Rules: [`../HISTORY.md`](../HISTORY.md).

## Set up agent documentation
**Date:** 2026-11-12 · **Time:** 19:05 · America/La_Paz

Created AGENTS.md, CLAUDE.md and the .docs architecture, code and git standards, project context, tech stack, validation, skills, persona, task planning and history documents from the project's sources. Recorded the modules, toolchain, checks and open questions that the project did not answer. No application code, build file or manifest was changed. Documentation checks cover unresolved placeholders, relative links and anchors, and line limits. No build or device test was run for this documentation-only change. No branch, commit or PR was created.

## Populate and verify the SDO harness baseline
**Date:** 2026-11-12 · **Time:** 19:06 · America/La_Paz

Populated the four root harness documents and .docs baselines from Gradle, manifest, Kotlin sources, CI and git. Added linked Firebase boundaries, test inventory, survey and task state. Owner confirmed smartphone hardware and no Jira; main comes from origin/HEAD, and the session supplies America/La_Paz. Specific device models remain Pending. Verified 21 Markdown files for placeholders, line limits, relative file/anchor links, exact CLAUDE/GEMINI content and sealed banners; all passed. AGENTS.md has 49 lines. Application build and runtime tests were not required or run for this documentation-only task. Full onboarding scripts are absent and earlier acceptance blockers remain. Existing unrelated modifications and deletions were preserved. No branch, commit, push or PR was created. Task handed off as InDevReview.

## Align active instructions and retire setup inputs
**Date:** 2026-11-12 · **Time:** 19:19 · America/La_Paz

Updated all 10 .agents workflows, 4 agent profiles and 3 gate checklists; added a shared project contract, native structural gate and regression fixtures. Retained required mechanical contracts in linked bounded documents. Updated root/git policy to honor the owner's automatic-commit preference after validation, with no automatic branch, push, PR, merge or release. Removed the setup prompt and superseded temporary setup task/survey; preserved product records, approvals, application files and existing tool deletions. Structural validation and 9 positive/negative contract cases passed; scoped whitespace checks passed. Android tests were not required for instruction-only changes; legacy harness tests remain unavailable because their scripts are absent. Handoff: InDevReview; full application onboarding remains unverified.

## Retire gates backed by absent scripts
**Date:** 2026-11-12 · **Time:** 22:31 · America/La_Paz

Removed obsolete legacy-script gates and their active workflow/skill/rule/prompt callers, deleted the four contract-inventory documents, and removed source_rules/coverage script commands from harness-project.json. Kept available Gradle/native checks, review criteria, real test evidence and approval requirements. The adaptive CI runner now uses existing Gradle/adb commands for the same test classes and retains logs/screenshots. Structural validation, 10 structural regression cases, 4 CI shell-control cases, JSON parsing, shell syntax and scoped whitespace checks passed. adb devices listed no connected runtime; actual Android execution remains unverified. Historical product/onboarding records and unrelated application/tooling changes were preserved. Task enters InDevReview; automatic signed-off commit follows the owner's preference, without push or PR.

## Always deliver authenticated test APKs
**Date:** 2026-11-12 · **Time:** 22:55 · America/La_Paz

Removed demo CI delivery and enabled authenticated debug builds on repository push/manual dispatch, reusing existing protected-input restoration and certificate verification before upload. Persisted the owner preference in root policy and harness docs. Structural/whitespace checks passed; Actions run 43355100969 at commit 78b7b99 passed Firebase/signing restoration, build, certificate verification, upload and cleanup. The authenticated artifact is available; interactive device sign-in was not tested. No application code or dependency version changed. Task handed off as InDevReview.

- 2026-11-13 10:38 America/La_Paz — Resume compact adaptive UI for draft developer review: compact dashboard/library selectors, session details, race guidance, metadata, sheet draft and lifecycle handling. Build and 248 JVM/44 Android component tests pass; quality/coverage remain non-passing, production IME/rotation and visual acceptance pending. Owner requested PR and authenticated signed CI APK for manual review. Skills: harness-router, android-ui-layer, android-testing, ui-verification, android-code-quality-checks. See docs/current/summary_v4.md.

- 2026-11-13 10:35 America/La_Paz — Draft PR #142 published for owner hands-on review; follow-up records real build/instrumented/quality evidence and capability UI deltas. The preceding entry's 10:38 timestamp was a transcription error; publication occurred before this entry. No acceptance claimed for failed gates or production IME/rotation.

- 2026-11-13 11:39 America/La_Paz — Owner-requested compact UI review repairs: remove panel quick Session/Master, restore angular cards/selectors and single campaign/tab outlines, remove clickable-row glyph mask, compact library counters/choices and localize kinds. RED confirmed angular geometry, unwanted shortcut, short-value ellipsis and mask pixel; focused GREEN 11 Android tests plus 248 JVM tests. CLI/adb captures retained and independently reviewed. Ktlint/Detekt/coverage remain non-passing; production IME/rotation and pixel-golden acceptance unproven. PR #142 signed CI delivery requested; full suite/build result follows in summary_v4.md.

- 2026-11-13 11:42 America/La_Paz — Compact UI revision validation handoff: full related Android suite 53/53 and JVM 248 pass; build/Android Lint pass (0 errors, 15 warnings). Independent review resolved; retain failed Ktlint/Detekt, 60.67% coverage and missing production IME/rotation/golden proof. Source commit b4bb5df; final authenticated APK linked in PR #142 after pipeline verification. InDevReview, owner acceptance pending.

- 2026-11-13 12:22 America/La_Paz — Focused owner follow-up: expose master operation quick action, center/reduce Cybergrunge bottom bar with 48dp targets, remove global red background line, relocate settings beside sync. Build and 4 affected Android tests pass; two CLI/adb captures inspected. No broad-suite rerun per owner request; prior static/coverage/runtime limitations retained. Skill: android-ui-layer/android-testing. InDevReview via PR #142.

- 2026-11-13 12:45 America/La_Paz — Make single-word Cybergrunge card titles shrink to available width without wrapping; existing autosize helper reused, no dependency. Scoped diff check; PR and remote build only per owner instruction, local tests/screenshots not run. Owner runtime verification pending; InDevReview via PR #142. Skill: android-ui-layer.

- 2026-11-13 15:06 America/La_Paz — Add owner-requested whole-word wrapping rule to CODE_STANDARDS (explicitly authorized), UI contract and design system; configure paragraph/no-hyphen shared typography. No new dependency. Diff checked; local tests/screenshots not run per owner preference, runtime fit/acceptance pending. PR #142 remote build; InDevReview.

- 2026-11-13 15:21 America/La_Paz — Repair session screenshot findings: full-width stacked equipment name/category/metadata, compact inset surfaces, themed body cards and auto-fitting failure count. No data/rule/dependency change. Diff check + remote build only per owner preference; no local tests/captures or runtime acceptance claimed. InDevReview via PR #142; skill android-ui-layer.

- 2026-11-13 15:27 America/La_Paz — Record owner's no-ellipsis UI rule in explicitly editable CODE_STANDARDS/UI contract. Require complete functional text via layout/word wrapping/legible autosize, never clipping. Current equipment/body fix retained. Docs-only diff check; no local tests/build/captures; InDevReview in PR #142.

- 2026-11-13 15:39 America/La_Paz — Remove all production ellipsis policies and associated line caps across app UI; common labels reflow/autosize, preserving words. Update existing label regression and reconcile UI/spec/design/README rules. Production scan zero ellipsis sites/literals; diff checked. Local tests/build/captures omitted under owner preference; remote CI/runtime pending. Signed-off chore: commit requested; InDevReview in PR #142. Skill android-ui-layer.

## 2026-11-13 20:18 America/La_Paz - Workflow cleanup - InDevReview

Owner-authorized cleanup removed raw logs, intermediate designs, disposable SDK/AVD and build caches; archived durable scratch handoff and relocated the PR worktree. Added scratch pre-commit enforcement, living backlog integration, concise risk-based workflow and senior agent configuration. Harness integrity and 11 contract + 4 hook + 4 runner cases passed; diff whitespace clean. Python helper tests unavailable on PATH; no Android execution for this documentation/workflow change. See docs/changes/harness-retro-2026-11-13-lean-workflow/retrospective.md. Developer acceptance pending.

- 2026-11-13 00:30 America/La_Paz — Compact UI visual review: restored existing Standard/Cybergrunge identity in Pen, applied single-line short controls and unnumbered quick access, preserved library choices. Fresh reopen/export: 41 frames, 39 original roots preserved, zero clipping/wrapped short text. Visual checkpoint InDevReview; design/implementation approvals and Android runtime verification pending. Skills: ux-design, pen-design-editor, adaptive. References: docs/current/UI_contract.md.


## Install Pen design CLI
**Date:** 2026-11-12 · **Time:** 23:43 · America/La_Paz

Installed the official @pen.dev/cli 0.3.16 globally with explicit owner authorization, using existing Node 26.13.0/npm 11.20.0. The version and interactive headless help commands passed. pen status exited 1 because no Pen session is authenticated; the owner must complete pen login before design operations. Recorded developer-tool availability in SKILLS and TECH_STACK. No application dependency or source changed; the larger UI task remains in progress pending login and design approval.

## 2026-11-13 20:55 America/La_Paz - Pending worktree reconciliation - InDevReview

Preserved valid Firebase setup, memory/knowledge/import provenance records and exact root-only documentation patch; integrated root Pen-tool commit without losing either audit stream. Archived all five prior tasks as Done after explicit owner acceptance, including UI; future product backlog remains open. Classified and reconciled stale root copies, restored useful tools/templates and removed three disposable artifacts after explicit scoped authorization. Both owned checkouts clean at 376198d before final record synchronization. Structural validation (45 documents), scoped whitespace and preservation hashes pass; no Android suites rerun for documentation/reconciliation. See docs/changes/worktree-reconciliation-2026-11-13/summary.md.

## 2026-11-13 21:02 America/La_Paz - PR142 rebase history correction - InDevReview

Owner approved correction of the rebase-only conflict. Preserve previous tip 5879d1d locally and consolidate the approved snapshot on current main; application tree must remain identical. Use explicit force-with-lease and synchronize clean owned checkouts. Validation is structural/whitespace and app-tree equality; no Android suite required for history-only correction. GitHub rebaseability is verified after publishing.

## 2026-11-13 22:12 America/La_Paz - Publish v0.51.1 - InDevReview

Published owner-requested patch release v0.51.1 from 6d85b83 using existing signed Android CI run 43540829981. Catalog validation, Firebase configuration restoration, release build, certificate comparison and APK publication passed. Release task records asset digest and recovery approach. No application code changed; prior approved UI/CI evidence reused, no new device smoke test or full suite claimed. Scoped documentation diff and pre-commit scratch enforcement checked before commit.

## 2026-11-14 00:26 America/La_Paz - Optimize workflows, skills, rules and tools for low token usage - InDevReview

Created branch feature/lean-workflows-and-tools from main (6d85b83, preserving all SDO Companion improvements) and compacted 11 rules, 6 workflows, 36 skills, 2 skill reference/example docs and 12 harness templates (~6,250 net lines removed, ~89% reduction across touched files). Fixed 7 broken relative links/anchors and harnesslens YAML frontmatter, removed retired harness/scripts references from templates, aligned harness-project.json (:app:ktlintCheck, :app:detekt), removed tracked .agents/workflows/.DS_Store, and extended validate-harness.ps1 to enforce line limits, placeholders, links, anchors and retired-script checks across .agents/rules, .agents/skills and harness/templates. validate-harness.ps1 (114 docs), validate-harness-tests.ps1 (11 cases), scratch-cleanup-tests.ps1 (4 cases) and whitespace checks passed; no Android build/runtime tests required for harness-only changes. See docs/changes/harness-retro-2026-11-14-token-optimization/retrospective.md.
