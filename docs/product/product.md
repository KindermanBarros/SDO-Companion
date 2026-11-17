# SDO Companion

Status: delivered review tasks accepted by the owner; unimplemented backlog remains open. Verification limits are recorded in [Memory](../MEMORY.md).

SDO Companion helps players create and manage RPG character sheets and use them in campaigns. The growing beta also includes a codex for the SDO RPG system. No additional release exclusions have been requested.

## Capability index

| Capability | Living requirements | Evidence status |
|---|---|---|
| Character sheets | [Character sheets](requirements/character-sheets.md) | Source and tests present; runtime acceptance unverified |
| Campaign use | [Campaigns](requirements/campaigns.md) | Source and tests present; runtime acceptance unverified |
| RPG codex | [Codex](requirements/codex.md) | Bundled catalog and screens present; runtime acceptance unverified |
| Synchronization & connectivity | [Synchronization](requirements/synchronization.md) | Done; owner accepted closure of BACKLOG-006/007. Recorded runtime verification limits remain unchanged |

## Delivery direction

First establish an accurate adoption baseline. Propose specific architecture, quality and test repairs using that evidence before selecting a new product feature. Detailed requirements belong in the capability files.

## Product Backlog

Requests recorded on 2026-11-12. Unimplemented items carry no fixed priority or delivery order. Details live in the capability requirements; this living backlog participates in workflow selection, execution, and reconciliation, though recording an item does not authorize implementation.

| ID | Item | Requirements | Status |
|---|---|---|---|
| BACKLOG-001 | Shared campaign journal | [Campaigns — CAMPAIGN-PLAN-001](requirements/campaigns.md#planned-changes) | Done |
| BACKLOG-002 | More complete, compact Session Mode with quick actions | [Campaigns — CAMPAIGN-PLAN-002](requirements/campaigns.md#planned-changes) | Done; owner closed delivered scope 2026-11-16; recorded limitations preserved |
| BACKLOG-003 | More complete, compact Historian/GM Mode with quick actions | [Campaigns — CAMPAIGN-PLAN-003](requirements/campaigns.md#planned-changes) | Done; owner closed delivered scope 2026-11-16; recorded limitations preserved |
| BACKLOG-004 | Character profile photos in sheets, lists, and play modes | [Character sheets — SHEET-PLAN-001](requirements/character-sheets.md#planned-changes) | Done; owner closed delivered scope 2026-11-16; recorded limitations preserved |
| BACKLOG-005 | Real-time shared battle grid in Session Mode | [Campaigns — CAMPAIGN-PLAN-004](requirements/campaigns.md#planned-changes) | ToDo; not implemented |
| BACKLOG-006 | Real-time simultaneous editing and online-only writes with read-only offline mode | [Synchronization — SYNC-PLAN-001](requirements/synchronization.md#planned-changes) | Done |
| BACKLOG-007 | Firestore read/write & payload optimization (delta sync, audit limits, dedupe character payload, parallel/batched queries) | [Synchronization — SYNC-PLAN-002](requirements/synchronization.md#planned-changes) | Done |
| BACKLOG-008 | Latest-field realtime sheet convergence | [Synchronization - SYNC-PLAN-003](requirements/synchronization.md#sync-plan-003---latest-field-realtime-sheet-convergence) | Done; owner closed delivered scope 2026-11-16; recorded limitations preserved |
| BACKLOG-009 | Server-ordered collaboration and per-item/list merges | [Synchronization follow-up](requirements/synchronization.md#remaining-collaboration-backlog) | Done; owner closed delivered scope 2026-11-16; recorded limitations preserved |
| BACKLOG-010 | Canonical ammo/charge consumption and automated item effects | [Session follow-up](requirements/campaigns.md#session-follow-up) | Done; owner closed delivered scope 2026-11-16; recorded limitations preserved |
| BACKLOG-011 | Hybrid RTDB sheet sync, durable offline commands and migration of all current sheets | [Synchronization](requirements/synchronization.md#sync-plan-004---hybrid-firebase-refactor) | Superseded by BACKLOG-013 (Spark Firestore Protocol v2); Functions/RTDB sheet mirrors retired |
| BACKLOG-012 | Cost-effective concurrent sheet sync and cloud-only Painel sync animation | [Synchronization - SYNC-PLAN-005](requirements/synchronization.md#sync-plan-005---small-group-concurrent-sync-and-painel-feedback) | Done; owner closed delivered scope 2026-11-16; recorded limitations preserved |
| BACKLOG-013 | 100% Spark Firestore Protocol v2 (single Firestore authority, client transactions, per-field/item/receipt Rules, Firestore presence, local original media + <=64KiB thumbnail, Room v33 account-scoped intents, isolated RTDB grid ACL) | [Synchronization — SYNC-PLAN-006](requirements/synchronization.md#sync-plan-006---spark-firestore-protocol-v2) | Done; owner closed delivered scope 2026-11-16; recorded limitations preserved |
| BACKLOG-014 | Sync animation using rotating arrows on the sync icon (`Icons.Default.Sync`) in Dashboard and sync banner | [UI / Dashboard](../design/design_system.md) | ToDo; not implemented |
| BACKLOG-015 | Attractive non-editable Path ("Caminho") visual using styled colors and typography when collapsed/read-only | [Character sheets](requirements/character-sheets.md) | InDevReview |
| BACKLOG-016 | Visual upgrade for Powers (`Poderes`) and Mystic (`Magias / Runas / Cinzas`) character sheet tabs | [Character sheets](requirements/character-sheets.md) | InDevReview |
| BACKLOG-017 | Session Mode visual upgrade and ability favoriting support in Session Mode | [Campaigns — Session Mode](requirements/campaigns.md) | ToDo; not implemented |
| BACKLOG-018 | Historian/GM Mode (`Modo Mestre`) interaction adjustments (prevent action buttons from only opening a single modal) | [Campaigns — Historian Mode](requirements/campaigns.md) | ToDo; not implemented |
| BACKLOG-019 | Remove campaign spoilers (e.g., silos and current-plot references) from the local database/catalogs, validated against `projetos/solidao-dos-oprimidos` lore repository | [Product overview](product.md) | ToDo; not implemented |
| BACKLOG-020 | Add lore-grounded powers, mystic abilities, special knowledges, paths, and items from the world's past (`projetos/solidao-dos-oprimidos`) without spoiling current campaign events | [Product overview](product.md) | ToDo; not implemented |
| BACKLOG-021 | Styled non-editable preview for Personal Notes (`Registros Pessoais`) when collapsed/read-only, matching the `BACKLOG-015` Path visual upgrade | [Character sheets](requirements/character-sheets.md) | InDevReview |

Owner closure 2026-11-16 23:32 America/La_Paz: ongoing delivered tasks are closed by explicit request. BACKLOG-005 remains ToDo; BACKLOG-011 remains superseded. Unimplemented scope, coverage improvement, authenticated multi-device verification and production cutover remain pending; closure does not claim those checks or operations occurred. See [Memory](../MEMORY.md). Earlier dated dispositions below are historical.

BACKLOG-001 selected by owner on 2026-11-14; implementation, security rules and tests accepted as Done by owner 2026-11-14 16:03; prior runtime verification limits preserved. [Memory](../MEMORY.md).
BACKLOG-006 selected by owner on 2026-11-14; real-time snapshot sync, same-account auto-convergence, online-only mutations, read-only offline mode and connection banner accepted as Done by owner 2026-11-14 16:03. [Memory](../MEMORY.md).
BACKLOG-007 selected by owner on 2026-11-14 on `feature/backlog-007-firestore-optimization`; canonical character payload deduplication, automatic migration of previous-version records, incremental delta sync cursors (`campaignAudit` bounded to 200, `campaignLibrary`), parallelized per-campaign queries, no-op write skipping, and Firestore rules unlink updates accepted as Done by owner 2026-11-14 16:03. [Memory](../MEMORY.md).

Owner correction 2026-11-14: journal Pen designs rejected for styleguide noncompliance; explicitly requested enforcing the styleguide in the Pen skill. [Memory](../MEMORY.md): redesigned journal approved by owner with minor Standard color refinement; home settings icon contrast fixed in Standard mode; implementation accepted as Done by owner 2026-11-14 16:03. No product priorities changed.

## Backlog workflow
Owner request 2026-11-13: publish the approved main-branch fixes as v0.51.1 using signed release CI. [Memory](../MEMORY.md): Done; owner accepted 2026-11-14; published with verified signed APK, CI run 43540829981 successful; existing product priorities unchanged.
Owner request 2026-11-14: create feature branch with Companion improvements and optimize workflows, skills, rules, templates and tools for lower token usage. [Memory](../MEMORY.md): Done; owner accepted 2026-11-14; on `feature/lean-workflows-and-tools`; ~6,250 lines removed across 73 harness files and validated by `validate-harness.ps1`.

- On start, select the authorized request and read its linked requirement; undefined priorities remain unordered.
- Add useful requests and findings with origin, state, and open questions without expanding the active task.
- On handoff, update the item to reflect pending, blocked, in-review, or accepted state; only the owner approves completion (`Done`).
- Visual review refinements do not complete the broader Session/GM mode scope.

## Approved deliveries
| Request | Record | Status |
|---|---|---|
| UI compaction and fixes | [Memory](../MEMORY.md) | Done; explicitly accepted 2026-11-13 |
| Workflow cleanup and lean workflow | [Memory](../MEMORY.md) | Done; explicitly accepted 2026-11-13 |

## Harness Feature Tracker

No complex work package is selected during onboarding. The empty tracker records no delivered or accepted features.

<!-- HARNESS_TRACKER_START -->
| ID | Feature | Workspace | Status | Updated | Notes |
|---|---|---|---|---|---|
<!-- HARNESS_TRACKER_END -->

Owner request 2026-11-14 15:46: consolidate documentation/design/agent ownership from the redundancy audit. [Memory](../MEMORY.md): Done; owner accepted closure 2026-11-14 16:03; one docs root, canonical design ownership and thin role adapters validated. Product behavior and existing priorities unchanged.

Prior closure applied only to delivered tasks. BACKLOG-002 was subsequently selected and is now InDevReview; BACKLOG-003 through BACKLOG-005 remain ToDo with unchanged scope. No remote publication is asserted.

Owner steering 2026-11-14 16:37: deliver player quick actions and latest-field realtime sheet convergence using the best supported existing path; defer remaining collaboration and consumable-state work. BACKLOG-002/008: InDevReview; final debug build passed, tests skipped by explicit owner request. BACKLOG-009/010: ToDo.

Owner request 2026-11-14 17:34: complete BACKLOG-009 and BACKLOG-010, explicitly including Cinza, with Room 30 preserved. Both are InDevReview; Firestore rules must be published with the client rollout. Test suites skipped per owner preference. Other backlog items are unchanged. [Memory](../MEMORY.md).


Campaign account membership repair (owner request 2026-11-14): InDevReview. Server-confirmed invite joins/rejoins and atomic GM sheet-owner enrollment implemented; Firestore emulator 40/40 PASS, authenticated Android debug CI passed; installed-account acceptance pending. [Campaign requirements](requirements/campaigns.md#account-membership-repair), [Memory](../MEMORY.md).

Firestore-only Spark solidification (owner request 2026-11-16): InDevReview (`BACKLOG-013`). Unifies sheet sync on Spark Firestore Protocol v2 (single Firestore authority, client transactions, per-field/item/receipt Rules, Firestore presence, local original media + <=64KiB thumbnail, Room v33 account-scoped intents, isolated RTDB grid ACL) and retires Cloud Functions and RTDB sheet mirrors. [Synchronization requirements](requirements/synchronization.md#sync-plan-006---spark-firestore-protocol-v2), [Memory](../MEMORY.md).

Owner report 2026-11-17: v0.54.1 synchronization permission recovery is InDevReview. Authorized production cutover completed: 10 sheets verified, 12 memberships/six invites converted, compatible Rules/indexes published and maintenance ended. Migrated-data permission regression passed in the emulator; installed-account retry remains owner verification. Existing backlog closure and BACKLOG-005 ToDo remain unchanged. [Memory](../MEMORY.md).

Owner report 2026-11-17: migrated sheets/portraits appear empty in v0.54.1. Hydration repair is InDevReview; cloud portraits are intact, activation receipt timestamps were normalized, and the client now repairs incomplete snapshots and accepts both receipt date encodings. Recovery/cache regressions, related engine/codec tests (23), debug build and lint passed; installed-account acceptance remains pending. Existing backlog remains unchanged. [Memory](../MEMORY.md).

Owner report 2026-11-17: empty-sheet creation was refused, then deleting it left an access-denied draft. Creation/read ordering and unpublished-delete queue recovery are InDevReview; 31 data tests, 36 Firestore tests and debug build passed. Repeated empty-account sync, orphan deletion recovery and later downloads are covered; existing product priorities and backlog closure remain unchanged. [Memory](../MEMORY.md).
