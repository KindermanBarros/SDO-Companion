# SDO Companion

Status: onboarding draft, pending approval. Product intent comes from setup_prompt.md; implementation observations are not release acceptance.

SDO Companion helps players create and manage RPG character sheets and use them in campaigns. The growing beta also includes a codex for the SDO RPG system. No additional release exclusions have been requested.

## Capability index

| Capability | Living requirements | Evidence status |
|---|---|---|
| Character sheets | [Character sheets](requirements/character-sheets.md) | Source and tests present; runtime acceptance unverified |
| Campaign use | [Campaigns](requirements/campaigns.md) | Source and tests present; runtime acceptance unverified |
| RPG codex | [Codex](requirements/codex.md) | Bundled catalog and screens present; runtime acceptance unverified |
| Synchronization & connectivity | [Synchronization](requirements/synchronization.md) | Implemented in `InDevReview` (`BACKLOG-006`); emulator/device runtime verification pending |

## Delivery direction

First establish an accurate adoption baseline. Propose specific architecture, quality and test repairs using that evidence before selecting a new product feature. Detailed requirements belong in the capability files.

## Product Backlog

Requests recorded on 2026-11-12. Unimplemented items carry no fixed priority or delivery order. Details live in the capability requirements; this living backlog participates in workflow selection, execution, and reconciliation, though recording an item does not authorize implementation.

| ID | Item | Requirements |
|---|---|---|
| BACKLOG-001 | Shared campaign journal | [Campaigns — CAMPAIGN-PLAN-001](requirements/campaigns.md#planned-changes) |
| BACKLOG-002 | More complete, compact Session Mode with quick actions | [Campaigns — CAMPAIGN-PLAN-002](requirements/campaigns.md#planned-changes) |
| BACKLOG-003 | More complete, compact Historian/GM Mode with quick actions | [Campaigns — CAMPAIGN-PLAN-003](requirements/campaigns.md#planned-changes) |
| BACKLOG-004 | Character profile photos in sheets, lists, and play modes | [Character sheets — SHEET-PLAN-001](requirements/character-sheets.md#planned-changes) |
| BACKLOG-005 | Real-time shared battle grid in Session Mode | [Campaigns — CAMPAIGN-PLAN-004](requirements/campaigns.md#planned-changes) |
| BACKLOG-006 | Real-time simultaneous editing and online-only writes with read-only offline mode | [Synchronization — SYNC-PLAN-001](requirements/synchronization.md#planned-changes) |

BACKLOG-001 selected by owner on 2026-11-14; implementation, security rules and tests handed off InDevReview (owner runtime/emulator acceptance pending). [Memory](../MEMORY.md).
BACKLOG-006 selected by owner on 2026-11-14; real-time snapshot sync, same-account auto-convergence, online-only mutations, read-only offline mode and connection banner handed off InDevReview. [Memory](../MEMORY.md).

Owner correction 2026-11-14: journal Pen designs rejected for styleguide noncompliance; explicitly requested enforcing the styleguide in the Pen skill. [Memory](../MEMORY.md): redesigned journal approved by owner with minor Standard color refinement; home settings icon contrast fixed in Standard mode; implementation handed off InDevReview. No product priorities changed.

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
