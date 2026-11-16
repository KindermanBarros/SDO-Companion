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
| BACKLOG-002 | More complete, compact Session Mode with quick actions | [Campaigns — CAMPAIGN-PLAN-002](requirements/campaigns.md#planned-changes) | InDevReview; player quick actions implemented |
| BACKLOG-003 | More complete, compact Historian/GM Mode with quick actions | [Campaigns — CAMPAIGN-PLAN-003](requirements/campaigns.md#planned-changes) | InDevReview; build passed, tests skipped by owner |
| BACKLOG-004 | Character profile photos in sheets, lists, and play modes | [Character sheets — SHEET-PLAN-001](requirements/character-sheets.md#planned-changes) | InDevReview; build passed, tests skipped by owner |
| BACKLOG-005 | Real-time shared battle grid in Session Mode | [Campaigns — CAMPAIGN-PLAN-004](requirements/campaigns.md#planned-changes) | ToDo; not implemented |
| BACKLOG-006 | Real-time simultaneous editing and online-only writes with read-only offline mode | [Synchronization — SYNC-PLAN-001](requirements/synchronization.md#planned-changes) | Done |
| BACKLOG-007 | Firestore read/write & payload optimization (delta sync, audit limits, dedupe character payload, parallel/batched queries) | [Synchronization — SYNC-PLAN-002](requirements/synchronization.md#planned-changes) | Done |
| BACKLOG-008 | Latest-field realtime sheet convergence | [Synchronization - SYNC-PLAN-003](requirements/synchronization.md#sync-plan-003---latest-field-realtime-sheet-convergence) | InDevReview; build passed, tests skipped by owner |
| BACKLOG-009 | Server-ordered collaboration and per-item/list merges | [Synchronization follow-up](requirements/synchronization.md#remaining-collaboration-backlog) | InDevReview; build-only validation |
| BACKLOG-010 | Canonical ammo/charge consumption and automated item effects | [Session follow-up](requirements/campaigns.md#session-follow-up) | InDevReview; build-only validation |
| BACKLOG-011 | Hybrid RTDB sheet sync, durable offline commands and migration of all current sheets | [Synchronization](requirements/synchronization.md#sync-plan-004---hybrid-firebase-refactor) | InDevReview; implementation, rules, backend reducer, migration tooling & dry run verified |
| BACKLOG-012 | Cost-effective concurrent sheet sync and cloud-only Painel sync animation | [Synchronization - SYNC-PLAN-005](requirements/synchronization.md#sync-plan-005---small-group-concurrent-sync-and-painel-feedback) | InDevReview; concurrent merge, minimal Firestore/RTDB patches, and cloud-only Painel animation implemented & verified |

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
