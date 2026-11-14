# Campaigns

**Capability ID**: `campaigns`

Status: adoption draft, pending approval.

## User goal

Players use their RPG character sheets in campaigns.

## Requirements

| ID | Product intent | Evidence qualification |
|---|---|---|
| CAMPAIGN-001 | Players can use character sheets in campaigns. | Requested in setup_prompt.md; campaign domain models, SyncedCampaignRepository and SessionModeScreen exist; execution not verified |

Source observations include invitations, membership, roles and session operations. Their full behavior is not newly specified or accepted during setup. Preserve existing contracts and data until a separate approved change defines any delta.

## Compact UI review

Compact UI review: dashboard filters use compact modal choices and session abilities retain complete expandable rules, costs and use actions. See [approved UI contract](../../current/UI_contract.md). Component regressions pass; owner installed-app acceptance pending.

## Planned changes

Backlog requested by owner on 2026-11-12. These are unimplemented proposals; each item needs refinement, specification, design, and acceptance criteria before delivery. They do not represent approval of a technical solution or certification of current behavior.

### CAMPAIGN-PLAN-001 — Shared campaign journal

Add a shared campaign journal. Define authorship, read/write permissions, entry ordering, and edit/sync behavior during refinement. Distinguish the journal from personal character notes and the existing technical operation history.

Refinement started 2026-11-14. Owner confirmed shared text entries, member creation, author edit/delete and GM moderation; owner approved specification and redesigned UI with minor Standard color refinement; implementation, Firestore rules and tests delivered InDevReview (device runtime and local emulator acceptance pending). See [Memory](../../MEMORY.md).

### CAMPAIGN-PLAN-002 — Session Mode evolution

Improve the UI and complete Session Mode features, focusing on quick actions and compact presentation while preserving all gameplay-relevant information. Define the inventory of actions, essential info, shortcuts, and states from existing flows; the request does not fix specific controls yet.

### CAMPAIGN-PLAN-003 — Historian / GM Mode evolution

Improve the UI and complete Historian/GM Mode features, focusing on quick actions, compact controls, and access to all information needed to run the campaign. Refine which info must stay visible and which actions need immediate access without assuming permission changes.

### CAMPAIGN-PLAN-004 — Real-time battle grid

Add a shared combat board over a grid in Session Mode where the GM can draw in real time and move characters represented by coin-shaped tokens with their profile photos.

Initial requested scope:

- Grid and freehand drawing on the board with real-time shared updates.
- Character movement via coin tokens using the profile photos from SHEET-PLAN-001.
- Movement distance calculation.
- Representation and handling of high/low elevation terrain.
- Future combat feature expansion, still open-ended; "many other things" is not treated as closed scope.

Decisions for refinement: grid scale/units, diagonals, and SDO movement rules; elevation/terrain meaning and effects; who can move each token; drawing tools; map persistence; concurrency, reconnection, and offline inspection. Offline edits remain forbidden under [SYNC-PLAN-001](synchronization.md#planned-changes), which also applies to the journal and Session/GM modes. Range, line of sight, obstacles, initiative, rule automation, and other tools may be evaluated later but are not confirmed by the current request.

Identified product dependencies: character profile photos for tokens; integration with campaigns and Session Mode. Delivery order and sync technology are not fixed here.

## References

- `setup_prompt.md`
- `app/src/main/java/com/kinderman/sdo/domain/model/Campaign.kt`
- `app/src/main/java/com/kinderman/sdo/data/repository/SyncedCampaignRepository.kt`
- `app/src/main/java/com/kinderman/sdo/presentation/session/SessionModeScreen.kt`

Owner review repair: preserve existing contracts while fixing theme outlines, clipped text and compact controls. Panel quick Session/Master removed; navigation retained. See [revision evidence](../../current/summary_v4.md). Owner explicitly accepted this UI delivery on 2026-11-13; automated evidence limitations remain in the linked summary.

Master operation exposes the existing quick-action dialog through a visible button on each collapsed character card; no command rules changed.
