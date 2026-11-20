# Campaigns & Play Modes

**Capability ID**: `campaigns` — see [Backlog](../product.md#product-backlog) and [Memory](../../MEMORY.md).

## Delivered Scope

- **Campaigns & Membership**: `campaigns/{id}`, `members/{uid}` (`ACTIVE|LEFT|REMOVED`), and `invites/{inviteId}` (`PENDING|CONSUMED|REVOKED`) with atomic invite consumption and GM character linking.
- **Shared Campaign Journal (`CAMPAIGN-PLAN-001`, `Done`)**: shared campaign entries (`campaignJournal`) with member creation, author edit/delete, and GM moderation.
- **Session Mode (`CAMPAIGN-PLAN-002`, `BACKLOG-010`, `BACKLOG-017`, `Done`)**: compact player dashboard with resource controls, favorite/available ability filters, confirmed dose/ammo/charge/Ash consumption, condition toggles, and equipment state switches.
- **Historian / GM Mode (`CAMPAIGN-PLAN-003`, `BACKLOG-018`, `Done`)**: party telemetry strip, alert filters, compact `OperationalCharacterCard`s, inline quick-action triggers, and `QuickActionsDialog`.

## Planned changes

### CAMPAIGN-PLAN-004 — Real-time shared battle grid (`BACKLOG-005`, `ToDo`)

- Shared grid and freehand board drawing in Session Mode using isolated RTDB `grid/{campaignId}/{gridId}` (`acl/{uid}`).
- Coin-token character movement using `SHEET-PLAN-001` avatars, movement distance calculation, and high/low terrain elevation.
- Open refinement items before implementation: SDO grid scale/diagonal rules, terrain mechanics, token move permissions, drawing tools, and persistence lifecycle.
