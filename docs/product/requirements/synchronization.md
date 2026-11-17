# Synchronization & Connectivity

**Capability ID**: `synchronization`

Status: delivered changes accepted by the owner; unimplemented planned changes remain open. See [Memory](../../MEMORY.md) and the [backlog](../product.md#product-backlog). Existing evidence limits remain unchanged.

## User goal

Preserve durable offline sheet edits and converge participating sessions through online Firestore transactions. Campaign administration remains online-only.

## Requirements

The existing app has local persistence, sync, and conflict handling per onboarding records. This document does not certify legacy runtime behavior beyond verified tests.

## Planned changes

### SYNC-PLAN-001 — Simultaneous editing and online-only policy

Historical accepted policy; the sheet-editing restriction is superseded by SYNC-PLAN-006.

Direct owner request: add real-time simultaneous editing; historically reserve conflict resolution for edits from different accounts (superseded by SYNC-PLAN-003 automatic latest-field policy); require online connection to edit, with connection/failure states and read-only offline mode without any edits.

| ID | Requested behavior |
|---|---|
| SYNC-PLAN-001-A | Propagate simultaneous edits in real time across participating sessions. |
| SYNC-PLAN-001-B | Edits from the same account, including across different sessions/devices, must converge without requiring manual conflict resolution. |
| SYNC-PLAN-001-C | Superseded by SYNC-PLAN-003: automatically prefer the latest field revision across accounts; do not prompt merely because multiple fields differ. |
| SYNC-PLAN-001-D | Require online connection to edit across the entire app. When offline, no flow may allow changes, including sheets, campaigns, journal, photos, session actions, drawing, and token movement. |
| SYNC-PLAN-001-E | Display connection and connection-failure states/screens, distinguishing unavailability from a completed operation. |
| SYNC-PLAN-001-F | Offer read-only offline mode without allowing any edits. Do not create queued offline edits for later upload as an alternative to blocking. |

Read-only offline inspection is the sole exception to mandatory online use; it grants no write permission. This policy applies to journal, Session/GM modes, photos, and battle grid refinements.

Open refinement items: which data sets remain cached for offline inspection; effective backend reachability criteria beyond network presence; handling in-progress edits when connection drops; confirmation/recovery for uncertain writes; same-field simultaneous edits by the same account; cross-account conflict granularity/comparison UI; write authorization and resuming edits upon reconnect.

### SYNC-PLAN-002 — Firestore data, query, and payload optimization

Selected by owner on 2026-11-14 (`BACKLOG-007`; implemented and owner-accepted as `Done` on `feature/backlog-007-firestore-optimization`):

| ID | Optimization target |
|---|---|
| SYNC-PLAN-002-A | Replace unbounded full-collection `.get()` syncs with incremental `updatedAt` / `createdAt` delta cursors (`maxSyncedAuditAt`, `maxSyncedLibraryAt`) and bounded `campaignAudit` pagination/limits (`AUDIT_SYNC_LIMIT = 200`, reusing existing composite indexes). |
| SYNC-PLAN-002-B | Remove duplicated legacy projections (`protections`, `notes`, `bodyRegions`, `organs`, `conditions`) and `@Exclude` local-only flags (`dirty`, `lastSyncedAt`) from Firestore `characters` documents while automatically converting previous-version records via `CharacterMigrationManager` and `optimizedForFirestore()`. |
| SYNC-PLAN-002-C | Parallelize per-campaign Firestore queries (`coroutineScope { ... async { ... }.awaitAll() }`), eliminate duplicate campaign document reads, skip no-op character transaction writes when semantic fields match, and allow `lastEditedBy` + legacy projection cleanup in `firestore.rules` unlink operations. |

### SYNC-PLAN-003 - Latest-field realtime sheet convergence

InDevReview (historical baseline superseded by Spark Firestore Protocol v2). Introduced `fieldUpdatedAt` (Room v30), per-element/property conflict keys, tombstones, and logical revisions for Firestore transactions without trusting client clocks.

#### Remaining collaboration backlog

BACKLOG-009 (InDevReview; superseded into Spark Protocol v2): per-item/property patches, stable ID tombstones, logical sequence revisions, and atomic arithmetic resource deltas with idempotent operation receipts.

## References

- Direct owner request (2026-11-12, 2026-11-15, 2026-11-16) and `docs/current/onboarding-summary.md`.
- [Campaigns & play modes](campaigns.md#planned-changes) and [Character photos](character-sheets.md#planned-changes).

### SYNC-PLAN-004 - Hybrid Firebase refactor

Superseded on 2026-11-16 by Spark Firestore Protocol v2 (`SYNC-PLAN-006`). Cloud Functions (`submitSheetCommand`, `projectSheetAudit`), RTDB sheet mirrors (`sheetAggregates`, `userSheets`), RTDB sheet presence, `sheetRoutes`, and `sheetMigration` are retired.

### SYNC-PLAN-005 - Small-group concurrent sync and Painel feedback

Owner request on 2026-11-16 (`BACKLOG-012`); InDevReview. Target ~10 users on Firebase Spark: concurrent independent field/item edits, reduced write payloads, and cloud-only Painel sync animation (`cloudSyncing`) separate from local pending/error banners.

### SYNC-PLAN-006 - Spark Firestore Protocol v2

Owner request on 2026-11-16: unify synchronization on 100% Firebase Spark without Functions, Blaze, Cloud Storage, or RTDB sheet mirrors.

| Area | Spark v2 policy |
|---|---|
| Authority & transactions | Firestore is the single authority; Android runs `FirebaseFirestore.runTransaction` against `characters/{id}`, `fields/{fieldKey}`, `items/{itemId}`, `items/{itemId}/fields/{fieldKey}`, and immutable `operations/{opId}` receipts mutually validated via `getAfter` in Security Rules (`<=10` accesses/rule, `<=20`/tx). |
| Room v33 outbox | Account-scoped `account_sheets`, `sheet_intents`, and `sheet_conflicts` (`MIGRATION_32_33`) track `DRAFT`, `PENDING`, `SENDING`, `CONFIRMED`, `CONFLICT`, `REJECTED`, `ACCESS_DENIED`, and `PAUSED_DEPENDENCY` states across restarts/logouts; WorkManager drains online. |
| Merge, deltas & conflicts | Independent fields and item properties merge automatically; same-field collisions surface base/local/remote conflict resolution (`baseRevision = remoteRevision`); tombstones prevent resurrection; dependent intents pause on conflict/rejection/access denial while independent edits proceed; resource deltas enforce bounds in Rules without clock-skew winners. |
| Presence, media & grid | Presence uses Firestore `characters/{id}/presence/{uid}_{connId}` (`expiresAt <= request.time + 120s`, no `onDisconnect`); original photos stay local (`localPhotoUri`) with optional `<= 64 KiB` thumbnail doc (`media/avatar`); RTDB keeps only isolated `grid/{campaignId}/{gridId}` with RTDB-only `acl/{uid}`. |
| Migration & rollback | Local administrative migration (`firebase/migration/`) validates round-trip parity with backup/comparison; no runtime Functions dependency. |

Automated actions require Rules-expressible invariants. Unsupported mechanical-effect consumption, dose/AshCost activation, and positive resource changes without a stored ceiling require explicit manual adjustment; Kotlin calculation alone is not a security guarantee. Corruption is bounded to 100. Deployment and data cutover are coordinated manually using the [operator procedure](../../firebase-deploy.md).

Conflict resolution offers local, online, and hand-edited values. The manual editor starts with the local value, preserves scalar/resource types and validates input; complex structured payloads remain editable through the sheet. Applying a merge creates a new intent using the recorded remote revision; further remote changes can produce another conflict.

Discovery must hydrate complete confirmed sheets, including portrait media, for new or incomplete local snapshots. Complete unchanged snapshots reuse their cached data after root/latest-receipt checks; a changed receipt triggers full refresh. Receipt times accept both migrated epoch milliseconds and Firestore timestamps.
