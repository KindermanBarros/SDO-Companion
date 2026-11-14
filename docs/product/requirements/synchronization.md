# Synchronization & Connectivity

**Capability ID**: `synchronization`

Status: delivered changes accepted by the owner; unimplemented planned changes remain open. See [Memory](../../MEMORY.md) and the [backlog](../product.md#product-backlog). Existing evidence limits remain unchanged.

## User goal

Enable simultaneous real-time editing and prevent sync issues by requiring an online connection for every mutation.

## Requirements

The existing app has local persistence, sync, and conflict handling per onboarding records. This document does not certify legacy runtime behavior beyond verified tests.

## Planned changes

### SYNC-PLAN-001 — Simultaneous editing and online-only policy

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

Owner requested automatic preference for the latest field changes and permitted deferring remaining work. Status: InDevReview; final debug build passed; tests skipped at the owner's request.
- Keep Firestore listeners and transactions; switching storage alone does not fix merge semantics.
- Add fieldUpdatedAt timestamps to sheet records; local Room schema 30 migrates from 29 with an empty map default. This is additive compatibility with a local migration, not an application-domain schema reset.
- Stamp only edited conflict groups and merge independent changes. Equal revisions prefer the committed remote value; account identity and field count no longer trigger manual conflict prompts.
- Rebase delayed editor changes against their in-memory baseline. A response to an older upload cannot clear a newer pending change or revive a local deletion.
- Preserve coupled inventory/canonical-state groups and grow-only delivery receipts. Legacy records use document timestamps until field metadata exists; older clients still use legacy behavior.
- Publishing retains the existing automatic-sync setting. Current revision times are client-derived, not globally server-ordered. Full authenticated cross-device acceptance remains unverified.

#### Remaining collaboration backlog

BACKLOG-009 (ToDo): server-authoritative ordering under device-clock skew; per-item/subfield merges and deletion semantics inside inventories, conditions, attributes and notes; simultaneous arithmetic resource spending without lost increments; mixed-version rollout guarantees; backend reachability/uncertain-write recovery; authenticated multi-device load/conflict validation. Evaluate a dedicated Realtime Database path only if measured requirements cannot be met with Firestore. No such migration or external deployment is performed in this delivery.

## References

- Direct owner request on 2026-11-12 for simultaneous editing, cross-account-only conflicts, and mandatory online editing.
- `docs/current/onboarding-summary.md`: onboarding observations on Room, Firebase, and existing sync.
- [Campaigns & play modes](campaigns.md#planned-changes).
- [Character photos](character-sheets.md#planned-changes).
