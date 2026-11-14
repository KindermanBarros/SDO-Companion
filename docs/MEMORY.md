# Project Memory & Task History (`MEMORY.md`)

This single file (`docs/MEMORY.md`) centralizes all task history, active states (`InProgress` / `InDevReview`), completed deliveries (`Done`), and persistent project decisions for SDO Companion.
**Strict rule**: Never create separate history or task-state files under `.docs/HistoryRecords/`, `.docs/OngoingTasks/`, or `.docs/CompletedTasks/`. All history updates and task summaries belong exclusively here.

---

## Active / In-Review Tasks (`InProgress` / `InDevReview`)

| ID / Task | Status | Date (America/La_Paz) | Summary, Deliverables & Evidence |
|---|---|---|---|
| **BACKLOG-001 / CAMPAIGN-PLAN-001** — Shared campaign journal | `InDevReview` | 2026-11-14 08:37 | Domain/policy (`CampaignJournalEntry`, `CampaignJournalRepository`), Firestore repo (`campaignJournal`), security rules (`firestore.rules`), `JournalViewModel`, `JournalScreen` (Standard blue/cyan + Cybergrunge + IME + conflict/offline/delete/discard), `SdoApp`/`DashboardScreen` navigation, and tests. Passed: `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, `:app:testDebugUnitTest` (257/257), `:app:lintDebug`, Kover `59.71%`, `test:rules` (30/30 on Firestore Emulator), `JournalJourneyTest` (2/2 on API 37 emulator after Espresso `3.7.0` and `matchParentSize()` fix in `SdoScreenMasthead`). Limitation: `ktlintCheck`/`detekt` have baseline/style backlog on new files. |
| **Home settings gear contrast fix** | `InDevReview` | 2026-11-14 01:50 | Fixed `contentColor`/`tint` on the Home Settings icon button in Standard mode ([StandardControls.kt](../app/src/main/java/com/kinderman/sdo/ui/standard/components/StandardControls.kt), [DashboardScreen.kt](../app/src/main/java/com/kinderman/sdo/presentation/dashboard/DashboardScreen.kt)), preserving Cybergrunge. Commit `f5e0ba2`, `:app:assembleDebug` PASS. |
| **Single `MEMORY.md` & rules consolidation** | `InDevReview` | 2026-11-14 10:40 | Consolidated all task history and states into `docs/MEMORY.md`; removed separate files under `.docs/HistoryRecords/`, `.docs/OngoingTasks/`, and `.docs/CompletedTasks/`; updated `AGENTS.md`, `.docs/HISTORY.md`, `.docs/TASK_PLANNING.md`, rules, and `validate-harness.ps1`. |
| **Auto-sync on Historian Dashboard open** | `InDevReview` | 2026-11-14 11:29 | Added `LaunchedEffect(surface, appSession?.uid)` in [SdoApp.kt](../app/src/main/java/com/kinderman/sdo/presentation/SdoApp.kt) calling `appViewModel.sync()` whenever `AppSurface.HISTORIAN` opens so player sheets stay up to date. |
| **Journal shortcut in Sheet/Session & backstack** | `InDevReview` | 2026-11-14 11:40 | Added `MenuBook` icon in the top bar of [SessionModeScreen.kt](../app/src/main/java/com/kinderman/sdo/presentation/session/SessionModeScreen.kt) (next to Open Sheet) and [CharacterSheetScreen.kt](../app/src/main/java/com/kinderman/sdo/presentation/character/CharacterSheetScreen.kt) (next to Open Session), with full `backStack` (`surface|selectedId|selectedJournalCampaignId`) in [SdoApp.kt](../app/src/main/java/com/kinderman/sdo/presentation/SdoApp.kt). |
| **BACKLOG-006 / SYNC-PLAN-001** — Real-time sync & online-only editing | `InDevReview` | 2026-11-14 12:00 | Connectivity via `AndroidConnectivityMonitor` (`ACCESS_NETWORK_STATE`), offline mutation blocking without local queue + read-only mode in [AppViewModel.kt](../app/src/main/java/com/kinderman/sdo/presentation/AppViewModel.kt)/[SdoApp.kt](../app/src/main/java/com/kinderman/sdo/presentation/SdoApp.kt) (`SyncStatusBanner`), real-time Firestore listeners (`observeRemoteChanges`), `lastEditedBy` (`MIGRATION_28_29`), and same-account auto-convergence in [CharacterSyncConflict.kt](../app/src/main/java/com/kinderman/sdo/domain/model/CharacterSyncConflict.kt). Passed: `:app:testDebugUnitTest` (`263/263`), `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, `:app:lintDebug`, and `validate-harness.ps1`. |

---

## Completed Tasks (`Done`)

| Date | Task | Summary & Evidence |
|---|---|---|
| 2026-11-12 | **Harness Setup & Baseline** | Created `AGENTS.md`, `.docs/*`, and baseline inventory (21 validated docs). |
| 2026-11-12 | **Harness Alignment** | Aligned 10 workflows, 4 profiles, 3 gates, and `project-contract.md`; removed setup inputs; 9 contract cases PASS. |
| 2026-11-12 | **Obsolete Gate Removal** | Removed non-existent script gates; kept Gradle/native checks; 10 structural + 4 CI cases PASS. |
| 2026-11-12 | **Authenticated CI Builds** | Removed demo artifacts; enabled signed & verified debug APKs (CI run `43355100969`). |
| 2026-11-13 | **Compact UI Polish (PR #142)** | Compact Standard/Cybergrunge UI without ellipsis, whole-word wrapping, GM quick action, and session refinements (`53/53` Android, `248` JVM, `:app:lintDebug` PASS). Explicitly accepted 2026-11-13. |
| 2026-11-13 | **Workflow Cleanup & `.scratch/` Policy** | Added `.scratch/` policy, pre-commit hook, living backlog, and `laziest_senior_developer` agent; reconciled worktrees (`376198d`, PR #142 rebased). Explicitly accepted 2026-11-13. |
| 2026-11-14 | **v0.51.1 Release** | Published v0.51.1 from `6d85b83` via signed release CI (run `43540829981`). Explicitly accepted 2026-11-14. |
| 2026-11-14 | **Lean Workflows & Docs Optimization** | Created branch `feature/lean-workflows-and-tools`; compacted 73 harness files (`~6,250` lines removed); consolidated `docs/changes/` to 1 `.md` file per change (`d4bf5a7`, `9aa3a6b`); `validate-harness.ps1` + 15 contract/hook tests PASS. Explicitly accepted 2026-11-14. |

---

## Accumulated Context & Persistent Decisions

- **Single memory & history file**: All delivery history, change records, and task summaries live strictly in `docs/MEMORY.md` (separate `.md` files under `docs/changes/`, `.docs/HistoryRecords/`, `.docs/OngoingTasks/`, or `.docs/CompletedTasks/` are forbidden; binary design assets may live under `docs/changes/assets/`).
- **Mandatory pre-commit cleanup**: Run `tools/pre-commit-cleanup.ps1` before every commit (enforced by `.githooks/pre-commit` and `validate-harness.ps1` to block `.scratch`, `.codex-attachments`, `.worktrees`, `firestore-debug.log`, `.DS_Store`, `Thumbs.db`, `*.orig`/`*.rej`, and `previews-*`).
- **English repository files**: Keep all documentation, rules, memory, and code comments in English; only in-app UI copy/domain messages remain in Portuguese.
- **Android 17 (API 37) Espresso**: Keep `androidx.test:runner:1.7.0` and `androidx.test.espresso:espresso-core:3.7.0` in `app/build.gradle.kts` to avoid `NoSuchMethodException: InputManager.getInstance`.
- **Cybergrunge Masthead**: `CyberGrungeInterference` inside `SdoScreenMasthead` must use `Modifier.matchParentSize()` so `Canvas(Modifier.fillMaxSize())` does not expand over the whole screen.
