# SDO Companion — Product Overview

Android companion for creating and managing SDO RPG character sheets, running campaigns, and consulting the system codex. Active history and verification notes live in [Memory](../MEMORY.md).

## Capability Index

| Capability | Living requirements | Status |
|---|---|---|
| Character sheets | [Character sheets](requirements/character-sheets.md) | Active (`Room v36`, creation wizard, sheet sections 1–16) |
| Campaigns & play modes | [Campaigns](requirements/campaigns.md) | Active (campaigns, invites, journal, Session & GM modes) |
| RPG codex & rules | [Codex](requirements/codex.md) | Active (bundled catalogs + 1,853 atomic handbook rules) |
| Synchronization | [Synchronization](requirements/synchronization.md) | Active (100% Spark Firestore Protocol v2) |

## Product Backlog

| ID | Item | Requirements | Status |
|---|---|---|---|
| BACKLOG-005 | Real-time shared battle grid in Session Mode | [Campaigns — CAMPAIGN-PLAN-004](requirements/campaigns.md#planned-changes) | ToDo |
| BACKLOG-022 | Curate & verify `player_handbook_rules.json` in Biblioteca against canonical SDO rulebook | [Codex](requirements/codex.md) | InDevReview (1,853 atomic rules integrated) |
| BACKLOG-023 | Commit-on-blur debouncing, pending intent coalescing & transitive conflict resolution audit/fixes | [Synchronization](requirements/synchronization.md) | InDevReview |
| BACKLOG-024 | Fix admin field-update `PERMISSION_DENIED` (`ACCESS_DENIED`) regressions, condition `cond_`/`lcond_` split & admin Rules ACLs | [Synchronization](requirements/synchronization.md) | InDevReview |
| BACKLOG-025 | Deity system (16 vault sheets, spoiler ACLs, Pillars/Domain Acts/Marks/Ascension/Corruption), compact Library/Inventory filters & multi-select/category item fixes | [Codex](requirements/codex.md), [Character sheets](requirements/character-sheets.md), [Synchronization](requirements/synchronization.md) | InDevReview |
| BACKLOG-026 | Resolve deity review findings and define Presence capacity, deity weakening, recovery, growth, defeat, and divine death mechanics | [Codex](requirements/codex.md), [Character sheets](requirements/character-sheets.md) | InDevReview |
| BACKLOG-001..004, 006..010, 012..021 | Journal, Session/GM quick actions, avatars, Spark v2 sync, UI & spoiler-free lore upgrades | [Memory](../MEMORY.md) | Done (BACKLOG-011 superseded by BACKLOG-013) |

## Workflow Rules

- Select authorized work from this backlog, preserve unrelated priorities, and reconcile status (`ToDo` / `InProgress` / `InDevReview` / `Done`) at handoff.
- Only explicit owner approval transitions an item to `Done`.
