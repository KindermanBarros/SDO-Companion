# Firebase boundary

Observed SDK boundary, not a new wire protocol. See [ARCHITECTURE](../ARCHITECTURE.md).

FirebaseAuthRepository uses Credential Manager/Google ID and Firebase Auth; session callbacks remove their auth listener on flow close. Synced repositories use Firebase Firestore SDK calls and await.

## Collections

Source: app/src/main/java/com/kinderman/sdo/data/repository/FirestoreCollections.kt.

| Constant | Collection |
|---|---|
| USERS | users |
| CHARACTERS | characters |
| CAMPAIGNS | campaigns |
| CAMPAIGN_MEMBERS | campaignMembers |
| CAMPAIGN_INVITES | campaignInvites |
| CAMPAIGN_AUDIT | campaignAudit |
| CAMPAIGN_LIBRARY | campaignLibrary |
| CAMPAIGN_DELIVERIES | campaignDeliveries |
| CAMPAIGN_JOURNAL | campaignJournal |

Character sync uses the unified 100% Spark Firestore Protocol v2 (`protocolVersion = 2`, `schemaVersion = 2`). Firestore is the sole authoritative store for sheets, campaigns, presence, and avatar thumbnails (`FirebaseFirestore.runTransaction`); Cloud Functions (`firebase/functions/`), RTDB sheet mirrors (`sheetAggregates`, `userSheets`), RTDB sheet presence, `sheetRoutes`, `sheetMigration`, and Cloud Storage are retired.

## Authoritative field and security contracts

- DTO/entity fields and local outbox: `CharacterRecord.kt`, `CampaignRecords.kt`, `OperationRecords.kt`, `OwnerRecord.kt`, and `SheetSyncRecord.kt` (Room v36, account-scoped `account_sheets`, `sheet_intents`, `sheet_conflicts`) under `app/src/main/java/com/kinderman/sdo/data/local/`.
- Normalization and RPG contracts: typed Kotlin models and rules in `:core:domain` plus `LegacyFirestoreNormalizer.kt`.
- Repository and sync operations: `app/src/main/java/com/kinderman/sdo/data/repository/` and `SparkWireCodec.kt`.
- Security Rules: `firebase/firestore.rules` (Firestore) and `firebase/database.rules.json` (isolated RTDB grid).
- Deployment procedure: [Firebase deployment](../../firebase-deploy.md).
- Synchronization requirements: [Synchronization policy](../../product/requirements/synchronization.md#sync-plan-006---spark-firestore-protocol-v2).

No Bluetooth UUIDs, camera protocol, Cloud Storage bucket, or custom HTTP endpoint contract exists in the application.

## Spark Firestore Protocol v2 (`characters/{charId}`)

- **Root (`characters/{charId}`)**: allowlisted metadata (`id`, `ownerId`, `campaignId`, `authorizedCampaignId`, `deleted`, `schemaVersion`, `canonicalSchemaVersion`, `itemSchemaVersion`, `protocolVersion`, `lockType`, `isLocked`, `lockedBy`, `lockedAt`, `revision`, `lastOperationId`, `updatedBy`, `updatedAt == request.time`, `appliedDeliveryIds`).
- **Per-field (`characters/{charId}/fields/{fieldKey}`)**: typed allowlisted keys (`key`, `value`, `revision`, `lastOperationId`, `updatedBy`, `updatedAt == request.time`) with scalar, resource-map (`life`, `sanity`, `arcane`, `energy`, `destiny`, `exhaustion`, `corruption`), list (`<= 500`), and map (`<= 100`) bounds.
- **Items & properties (`characters/{charId}/items/{itemId}` and `items/{itemId}/fields/{fieldKey}`)**: typed item prefixes (`inv_`, `istate_`, `abil_`, `pow_`, `mys_`, `cond_`, `lcond_`, `note_`, `attr_`), logical tombstones (`deleted: bool` preventing resurrection by common writes), and independent property/`skill_*` subdocuments checked against `deleted == false` via `getAfter`.
- **Immutable receipts (`characters/{charId}/operations/{opId}`)**: create-only, authenticated actor, committed outcome, server timestamp, fixed operation formats and `targets` (`1..7`, at most five when item fields are involved). Targets include expected/next revision and relevant base/desired values. Mutual `getAfter` checks and executable budget tests enforce `<=10` accesses/rule and `<=20`/transaction. Client `MIGRATE_V2` is denied; administrative migration bypasses client Rules.
- **Presence (`characters/{charId}/presence/{uid}_{connectionId}`)**: sheet readers only; own-`uid` write/delete; `updatedAt == request.time`, `expiresAt <= request.time + 120s`; no `onDisconnect` (expired docs treated as absent).
- **Avatar thumbnail (`characters/{charId}/media/avatar`)**: optional thumbnail (`<= 65536` bytes, `<= 256x256`, sheet auth); original photos stay local (`localPhotoUri`).

Unverifiable mechanical-effect/dose automation is rejected before queueing; manual revision-compared adjustments remain available. Resource decrements preserve nonnegative balances; automatic increases require an explicit stored ceiling, with corruption capped at 100. Derived maxima are not inferred by Rules.

## Campaigns & isolated RTDB grid

- `campaigns/{campaignId}/members/{uid}` (`role == "PLAYER"`, `state: ACTIVE|LEFT|REMOVED`, `joinedByInviteId`) and `invites/{inviteId}` (`PENDING|CONSUMED|REVOKED`): invite acceptance atomically marks `CONSUMED` and activates `PLAYER` membership via `getAfter` (GM role is `campaigns/{id}.ownerId`, never self-assigned). GM `linkCharacter` preserves `ownerId` and activates owner `PLAYER` membership atomically; unlink sets `campaignId = ""` (owner allowed even when `ARCHIVED`; archived campaigns block normal sheet edits).
- **RTDB (`firebase/database.rules.json`)**: default deny; explicitly denies `sheetAggregates`, `userSheets`, and `presence`. Retains only isolated `grid/{campaignId}/{gridId}` with RTDB-only `acl/{uid}`, never consulting Firestore or authorizing sheets.
