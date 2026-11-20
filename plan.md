# SDO Companion — Data Models, Firebase Schemas, Legacy Compatibility & Offline-First Synchronization Plan

> **Scope:** Technical architecture & implementation plan grounded in direct inspection of `:core:domain`, `:app`, Room v35, `firebase/firestore.rules`, and a strictly read-only local mirror of production Firestore (`sdo-companion`, 9,637 documents exported on 2026-11-20). No application code or production data was modified.

---

## 1. Current Architecture Assessment & Real-Data Audit

### 1.1 Repository Codebase Audit

SDO Companion already runs **Spark Firestore Protocol v2** (`protocolVersion = 2`, `schemaVersion = 2`, 100% Spark-compatible with zero Cloud Functions / Cloud Storage / RTDB sheet mirrors). Inspecting `:core:domain` and `:app/data` reveals overlapping legacy abstractions and drift risks:

| Layer / File | Current Implementation | Identified Inconsistency / Risk |
|---|---|---|
| **Room (`AppDatabase.kt`, v35)** | Retains retired v1 tables (`sheet_routes`, `sheet_outbox`, `sheet_sequences` via `SheetSyncDao`) alongside active Spark v2 tables (`account_sheets`, `sheet_intents`, `sheet_conflicts`). | Duplicated persistence surface; risk of legacy code paths touching retired tables. |
| **Local Entity (`CharacterRecord.kt`)** | Carries Firestore POJO annotations (`@Exclude`, `@PropertyName`), legacy sync flags (`dirty`, `lastSyncedAt`, `fieldUpdatedAt: Map<String, Long>`), and dual representations (flat columns alongside `canonical*Payload` JSON blobs). | Conflates Room persistence with Firestore DTOs; flat and canonical columns can drift. |
| **Serialization (`SparkWireCodec.kt`, `CharacterConverters.kt`, `SheetSyncRecord.kt`)** | Mixes **Gson** (`SparkWireCodec`, `CharacterConverters`, `SparkFirestoreSyncEngine`) and **kotlinx.serialization** (`SparkSyncModels`, `SheetSyncRecord`). | Divergent null-handling, enum coercion, and default-value rules across layers. |
| **Conflict Logic (`CharacterSyncConflict.kt` vs `SparkSyncModels.kt`)** | `CharacterSyncConflict.kt` retains timestamp-LWW (`automaticRemoteFieldIds` via `fieldUpdatedAt`) alongside Spark v2's revision-based `SheetConflictItem`. | Timestamps suffer from clock skew and silent overwrites; dual conflict contracts create ambiguity. |
| **Remote Hydration (`FirestoreSparkRemoteGateway.kt`)** | `fetchSheetDocuments()` assumes every `characters/{charId}` doc is already Spark v2 (`fields/`, `items/`, `media/avatar`). | Unmigrated legacy v1 monolithic roots (`protocolVersion < 2` or absent) yield empty subcollections and default values. |
| **Unknown Fields (`CharacterMigrationManager.kt`)** | Captures invalid canonical JSON blobs in `NeedsReview`, but does not preserve unknown/unrecognized legacy or forward-compatible keys. | Unrecognized properties can be silently dropped during normalization round-trips. |

### 1.2 Production Firestore Mirror Audit (`sdo-companion`, Read-Only Snapshot)

A read-only mirror of production Firestore was exported to a gitignored directory (`.scratch/prod-mirror/snapshot.json`, **9,637 documents** across 6 root collections) and audited via `.scratch/audit-prod-mirror.mjs`:

| Collection / Pattern | Count | Verified Findings |
|---|---:|---|
| `characters/{id}` | 14 | 10 active, 4 soft-deleted; 4 campaign-linked, 10 standalone. All 14 have `protocolVersion=2`, `schemaVersion=2`, `canonicalSchemaVersion=2`, `itemSchemaVersion=8`, exact 18 root keys. |
| `characters/{id}/fields/{key}` | 595 | Active sheets have 49 fields (incl. `transports`, `familiars`); deleted sheets are sparse (2, 11, 46, 46). Zero unknown field keys. |
| `characters/{id}/items/{itemId}` | 419 | All match allowlisted prefixes (`inv_`, `istate_`, `abil_`, `pow_`, `mys_`, `cond_`, `lcond_`, `note_`, `attr_`). |
| `characters/{id}/items/{id}/fields/{key}` | 5,923 | Zero negative ints or non-int `intensity` anomalies. |
| `characters/{id}/operations/{opId}` | 2,198 | Immutable receipts (up to 349 on a single sheet). |
| `characters/{id}/media/{docId}` | 6 | All 6 `media/avatar` docs have valid `dataUri`, `mimeType`, `width`, `height`, `updatedBy`, `updatedAt: Timestamp`. |
| `characters/{id}/migration/v2` | 10 | Audit markers from admin v1->v2 cutover (`legacyChecksum`, `subcollectionChecksum`, `cutoverOperationIds`). |
| `campaigns/{id}` + `members/` + `invites/` | 6 / 12 / 6 | 1 `ACTIVE` campaign, 5 `DELETED` campaigns. |
| Legacy root `campaignMembers` / `campaignInvites` | 12 / 6 | **100% redundant** (`0` missing in `campaigns/{id}/members` & `invites`). |
| `users/{uid}` | 11 | **Schema drift**: 5 docs have `{displayName, email}` (missing `role`); 6 docs have `{displayName, email, role}`. |
| `campaignAudit/{id}` | 419 | Historical audit log entries. |

#### Critical Live Production Anomalies Discovered via Local Mirror

1. **LIVE DEADLOCK BUG (`characters/7b45ed0f-8827-429d-ade0-66aaeaf7e1ee`)**:
   - Root has `lockType = "PLAYER"`, `isLocked = true`, **but `lockedAt = null`**.
   - `keepsRootLock(before, after)` forces `after.lockedAt == null`, while `validCharacterRootShape(after)` requires `after.lockedAt is timestamp` when `lockType != 'NONE'`.
   - **Impact (reproduced live in emulator):** All root-level operations (`UNLINK_CAMPAIGN`, `DELETE_SHEET`, `TRANSFER_OWNERSHIP`, `ACCEPT_DELIVERY`) fail with `PERMISSION_DENIED` until `SET_LOCK(NONE)` resets the lock.
2. **Mixed Timestamp Wire Types from Admin Migration**:
   - **10/14** character roots, **490/595** `fields/*`, **5,631/5,923** `items/*/fields/*`, and **2,023/2,198** `operations/*` store `updatedAt`/`createdAt` as **`integerValue` (epoch ms)** instead of Firestore `timestampValue`.
   - Typed `getTimestamp(...)` calls without raw normalization will throw.
3. **Receipt Subcollection Growth**:
   - Up to 349 `operations/*` docs on a single sheet; routine hydration must never scan `operations/` (only direct `doc(opId)` lookups or `limit(1)` head checks).


---

## 2. Canonical Data Models & Schema-Versioning Strategy

### 2.1 Single Canonical Domain Contract (`:core:domain`)
- `com.kinderman.sdo.domain.model.Character` is the sole canonical model consumed by ViewModels and Compose UI.
- **Explicit Monotonic Version Fields:**
  - `protocolVersion` (`2`): Firestore Spark v2 decomposed wire layout (`root` + `fields/` + `items/` + `operations/` + `media/avatar`).
  - `schemaVersion` (`2`): Firestore Rules document shape version.
  - `canonicalSchemaVersion` (`CURRENT_CHARACTER_SCHEMA_VERSION`): Domain model schema version.
  - `itemSchemaVersion` (`CURRENT_ITEM_DATA_VERSION`): Inventory/item-state schema version.
  - `creationRulesVersion` (`CURRENT_CREATION_RULES_VERSION`): Character creation ruleset version.
- **Field Types, Nullability, Defaults & Bounds:**
  - IDs (`id`, `ownerId`, `campaignId`, `itemId`): Non-null strings, `campaignId == "default"` normalized to `""`, max 128 chars, safe-ID charset for entity IDs.
  - Resources (`life`, `sanity`, `arcane`, `energy`, `destiny`, `exhaustion`, `corruption`): Non-null `ResourceValue(current >= 0, maximum >= 0, adjustment: SignedInt32)`, with `corruption.current <= 100`.
  - Collections: Non-null `emptyList()` / `emptyMap()` defaults; bounded to Rules limits (`<= 500` list elements, `<= 100` map entries, strings `<= 250,000` chars).
  - Locks: `lockType in {NONE, PLAYER, HISTORIAN}`, `isLocked == (lockType != NONE)`; if `lockType == NONE` then `lockedBy == ""` and `lockedAt == null`, else `lockedBy` is a valid UID and `lockedAt` is a non-null timestamp.
  - **Unknown Field Preservation:** Add `unrecognizedFieldsJson: String = ""` to `Character`, `CharacterRecord`, and `fields/unrecognizedFieldsPayload` to preserve unknown legacy or forward-compatible keys verbatim.
  - **Forward-Compatibility Guard:** Any record with `canonicalSchemaVersion > CURRENT_CHARACTER_SCHEMA_VERSION` fails safely into read-only `DomainError.UnsupportedSchemaVersion` instead of overwriting newer data.

### 2.2 Mapping & Serialization Contract (Local <-> Domain <-> Firebase)
- **Domain (`:core:domain`)**: Pure Kotlin models, validation, diff/merge, and conflict classifiers (zero Room/Firebase dependencies).
- **Local (`:app/data/local`)**:
  - `CharacterRecord`: Pure Room `@Entity`. Remove `@Exclude` / `@PropertyName` Firestore annotations; deprecate/remove `dirty`, `lastSyncedAt`, `fieldUpdatedAt` once all sync metadata is read solely from `AccountSheetRecord` + `SheetIntentRecord`.
  - Retire `SheetRouteRecord`, `SheetOutboxRecord`, `SheetSequenceRecord`, and `SheetSyncDao` via Room migration `35 -> 36`.
- **Firebase Wire (`:app/data/sync/SparkWireCodec.kt`)**:
  - Single lossless bidirectional codec between `CharacterRecord` and `SparkSheetDocuments`.
  - Standardize canonical JSON payloads on `kotlinx.serialization.json.Json` (`ignoreUnknownKeys = true`, `encodeDefaults = true`, `coerceInputValues = true`) and phase out Gson adapters to eliminate dual-serializer drift.
  - Normalize all wire timestamps (Firestore `Timestamp`, epoch-ms `Long`, `Double`, ISO string) at the gateway boundary before decoding.

---

## 3. Legacy Data Compatibility & Normalization Pipeline

All external/historical inputs flow through one deterministic normalization boundary before reaching the UI:

1. **Wire Detection (`FirestoreSparkRemoteGateway.fetchSheetDocuments`)**:
   - Inspect `rootSnap.data["protocolVersion"]`.
   - `protocolVersion == 2`: Hydrate from `root` + `fields/` + `items/` + `media/avatar` via `SparkWireCodec.fromSparkDocuments()`, normalizing numeric `updatedAt`/`createdAt` and repairing missing sparse fields (`transports`, `familiars`, `attr_*` defaults).
   - `protocolVersion < 2` or absent (legacy monolithic v1 root): Route through `LegacyFirestoreNormalizer.normalizeMonolithicRoot(sheetId, rawMap)`:
     - Coerce mismatched types (string-encoded ints, `Double -> Int`, `null` lists/maps -> empty, `"default"` campaignId -> `""`, Portuguese `ItemQuality` labels -> enum).
     - Map deprecated/renamed properties (`notes` -> `personalNotes`, legacy flat `powers`/`inventory`/`conditions`/`bodyRegions`/`organs` -> canonical structures).
     - Store any unrecognized keys in `unrecognizedFieldsJson` and unparseable legacy mechanics verbatim in `migrationReviews: List<NeedsReview>`.
2. **Idempotent Schema Migration (`CharacterMigrationManager`)**:
   - Enforces uninterrupted version chain `0 -> 1 -> ... -> CURRENT_CHARACTER_SCHEMA_VERSION`.
   - Strictly pure & idempotent: `migrate(migrate(r)) == migrate(r)`.
   - Never mutates `unrecognizedFieldsJson` or drops `migrationReviews`.
   - Local read migrations run with `markDirty = false`; v1 -> v2 Firestore cutover remains administrative (client `MIGRATE_V2` stays denied by Rules).


---

## 4. Offline-First Synchronization Architecture

Consolidate all synchronization inside `SparkFirestoreSyncEngine` (no redundant sync layer):

1. **Immediate Local Persistence**:
   - Every user edit computes a deterministic diff against `AccountSheetRecord.confirmedJson` (replayed with active `PENDING`/`SENDING` intents), writes `SheetIntentRecord` rows in a Room `@Transaction`, and updates the projected `CharacterRecord` immediately so UI edits work offline with zero latency.
   - Multi-target mutations exceeding Spark Rules budgets (`> 7` targets, or `> 5` when `ITEM_FIELD` is included) are partitioned into ordered `SparkOperationBatch` chunks linked via `dependsOnOperationId`.
2. **Event-Driven & Background Drain**:
   - Triggered on local edit, connectivity restore (`AndroidConnectivityMonitor`), app foreground, and WorkManager (`NetworkType.CONNECTED`, exponential backoff), serialized per account via `drainMutex`.
3. **Idempotent, Retryable Transactions**:
   - Inside `FirebaseFirestore.runTransaction`:
     1. Read `characters/{charId}/operations/{opId}`. If already present and `verifyAgainst(intent) == MATCHED_COMMITTED`, mark `CONFIRMED` without re-applying (safe recovery after mid-flight network drop or duplicate trigger). If `HASH_MISMATCH`, mark `REJECTED`.
     2. Read target docs (`fields/{key}`, `items/{itemId}`, `items/{itemId}/fields/{key}`). Because `PATCH_FIELDS` does **not** touch the root `characters/{charId}` doc, Player and GM field edits have zero root contention.
     3. Validate `expectedRevision` and commit target writes + immutable `operations/{opId}` receipt (`FieldValue.serverTimestamp()`).
4. **Remote Pull & Real-Time Convergence**:
   - Active sheets monitor lightweight head changes and hydrate only when remote revisions advance, updating `AccountSheetRecord.confirmedJson` + `fieldRevisionsJson` and replaying still-pending intents via `applyPendingIntentsOnConfirmed`.

---

## 5. Conflict Detection & Resolution Algorithms

Retire legacy timestamp-LWW (`CharacterSyncConflict.kt` / `automaticRemoteFieldIds`) in favor of **per-target revision-based 3-way conflict handling**:

| Scenario | Granularity | Detection | Resolution Strategy |
|---|---|---|---|
| **Independent fields edited** (e.g., Player edits `field:life`, GM edits `field:story` or `item:note_*`) | `FIELD`, `ITEM`, `ITEM_FIELD` | Target `expectedRevision == remote.revision` despite other fields advancing. | **Automatic convergence:** transaction commits without conflict; confirmed snapshot merges both edits. |
| **Concurrent resource/stack deltas** (`ADJUST_RESOURCE`, `ITEM_CONSUME` with `TargetOp.DELTA`) | `FIELD` / `ITEM_FIELD` | Remote revision advanced, but op is a bounded commutative delta (`-5 HP`, `-1 dose`) with `[minValue, maxValue]`. | **Automatic rebase:** recompute `desired = clamp(remote + delta, min, max)` and retry at `remote.revision` if bounds hold; else raise conflict. |
| **Same field edited to identical value** | `FIELD` / `ITEM_FIELD` | `expectedRevision != remote.revision`, but `localDesiredValue == remoteCurrentValue`. | **Automatic no-op resolution:** mark intent `CONFIRMED` and advance local base revision. |
| **Same field edited to different values** | `FIELD` / `ITEM_FIELD` | `expectedRevision != remote.revision` and `localDesiredValue != remoteCurrentValue`. | **Explicit 3-way conflict:** mark intent `CONFLICT`, pause dependent intents (`PAUSED_DEPENDENCY`), persist `SheetConflictRecord` with `basePayloadJson`, `localPayloadJson`, and `remotePayloadJson`. User picks **Keep Local** (re-queues at `remoteRevision`), **Keep Remote** (discards intent), or **Custom**. |
| **Item tombstone vs edit** (`DELETE_ITEM` vs `UPSERT_ITEM`) | `ITEM` | Remote item has `deleted == true` while local intent targets `ITEM` / `ITEM_FIELD`. | **Explicit conflict:** Rules block resurrection by normal writes; surface `SheetConflictRecord` for explicit `RESTORE_ITEM` or discard. |
| **Permission / Lock / Archive change offline** | `SHEET` | Transaction returns `PERMISSION_DENIED` (e.g., GM set `HISTORIAN` lock, archived campaign, or removed player). | Transition intent to `ACCESS_DENIED` (preserves draft, stops retry loop); expose `retryAccessDeniedDraft` / `discardDraft`. |

---

## 6. Firebase Security Rules & Spark-Tier Compatibility

- **100% Spark Compatible**: Pure Firestore client SDK transactions, listeners, and Security Rules. No Cloud Functions, Cloud Storage, or RTDB sheet mirrors.
- **Required `firebase/firestore.rules` Changes**:
  1. Add `'unrecognizedFieldsPayload'` to the string allowlist in `validFieldValue(fieldKey, val)` (`<= 250000` chars).
  2. **Fix legacy `lockedAt == null` deadlock compatibility (or repair production doc):**
     - Repair `characters/7b45ed0f-8827-429d-ade0-66aaeaf7e1ee` in production (either via GM `SET_LOCK(NONE)` or admin backfill of `lockedAt: Timestamp`).
     - Optionally harden `keepsRootLock` / `validCharacterRootShape` or add a pre-flight client repair check if `isLocked && lockedAt == null`.
  3. Preserve all existing ownership, campaign membership (`campaigns/{id}/members/{uid}`), GM (`campaigns/{id}.ownerId`), Admin, lock (`NONE`/`PLAYER`/`HISTORIAN`), and `ARCHIVED` invariants.
  4. Re-verify Rules access budget tests (`<= 10` accesses/rule, `<= 20` accesses/transaction).


---

## 7. Affected Modules, Files & Responsibilities

| Module | File(s) | Responsibility |
|---|---|---|
| `:core:domain` | `domain/model/Character.kt` | Add `unrecognizedFieldsJson`; formalize canonical defaults & bounds. |
| `:core:domain` | `domain/model/SparkSyncModels.kt` | Define delta-rebase eligibility, no-op conflict classification, and target-key contracts. |
| `:core:domain` | `domain/model/CharacterSyncConflict.kt` | Remove legacy timestamp-LWW `automaticRemoteFieldIds`; retain only UI summary helpers if needed. |
| `:app` | `data/local/AppDatabase.kt`, `SheetSyncRecord.kt` | Room migration `35 -> 36`: add `unrecognizedFieldsJson` to `characters`; drop retired `sheet_routes`, `sheet_outbox`, `sheet_sequences`. |
| `:app` | `data/local/CharacterRecord.kt` | Strip Firestore `@Exclude`/`@PropertyName` annotations; add `unrecognizedFieldsJson`; phase out `dirty`/`lastSyncedAt`/`fieldUpdatedAt`. |
| `:app` | `data/local/CharacterMigrationManager.kt` | Guarantee strict idempotency and preserve `unrecognizedFieldsJson` + `migrationReviews` across all steps. |
| `:app` | `data/sync/LegacyFirestoreNormalizer.kt` *(new)* | Normalize unmigrated legacy v1 monolithic Firestore roots and mixed numeric/timestamp fields into canonical `CharacterRecord`. |
| `:app` | `data/sync/SparkWireCodec.kt` | Encode/decode `unrecognizedFieldsPayload`; consolidate canonical JSON onto `kotlinx.serialization`. |
| `:app` | `data/sync/FirestoreSparkRemoteGateway.kt` | Detect `protocolVersion < 2` in `fetchSheetDocuments()` and delegate to `LegacyFirestoreNormalizer`; normalize numeric timestamps everywhere. |
| `:app` | `data/sync/SparkFirestoreSyncEngine.kt` | Implement delta-rebase, identical-value no-op resolution, `PAUSED_DEPENDENCY` chaining, and `ACCESS_DENIED` draft preservation. |
| `firebase/` | `firebase/firestore.rules` | Allowlist `unrecognizedFieldsPayload`; verify emulator Rules & budget tests. |

---

## 8. Migration Rollout & Rollback Strategy

1. **Phase 0 — Production Data Hygiene (Safe Targeted Fix)**:
   - Unlock or backfill `lockedAt` on `characters/7b45ed0f-8827-429d-ade0-66aaeaf7e1ee` so root operations (`UNLINK_CAMPAIGN`, `DELETE_SHEET`, etc.) are no longer deadlocked.
   - Optionally normalize missing `role` on the 5/11 `users/*` docs and archive/delete the 100% redundant legacy root `campaignMembers` (12) and `campaignInvites` (6) collections after final backup verification.
2. **Phase 1 — Additive Firestore Rules Deployment**:
   - Deploy `firebase/firestore.rules` adding `'unrecognizedFieldsPayload'` to `validFieldValue`. Strictly backward-compatible with all deployed v2 APKs.
3. **Phase 2 — Client Release (Room 35 -> 36 + Normalizer + Engine Hardening)**:
   - `MIGRATION_35_36` runs `ALTER TABLE characters ADD COLUMN unrecognizedFieldsJson TEXT NOT NULL DEFAULT ''` and drops only retired v1 tables (`sheet_routes`, `sheet_outbox`, `sheet_sequences`).
   - Preserves `account_sheets`, `sheet_intents`, and `sheet_conflicts` intact so queued offline edits survive upgrade.
4. **Rollback Considerations**:
   - Rules changes are purely additive and `protocolVersion = 2` wire layout is unchanged; older v2 APKs continue operating cleanly and safely ignore `unrecognizedFieldsPayload`.
   - Client regressions can be resolved via hotfix APK without Firestore data rollback.


---

## 9. Local-Mirror Live Verification Results (Executed)

A local Firestore Emulator (`sdo-companion-mirror`) was seeded with real mirrored production documents from `.scratch/prod-mirror/snapshot.json` and tested against the actual `firebase/firestore.rules` via `.scratch/test-prod-mirror-emulator.mjs` (**7/7 PASS**, 5.67s):

| # | Live Mirror Test Case | Result | Verified Behavior |
|---|---|---|---|
| 1 | Real prod sheet read auth (`245a3213-...`) | **PASS** | Owner, Campaign GM, Admin read root/fields/media; Stranger denied. |
| 2 | Migrated sheet `PATCH_FIELDS` (`integerValue` `updatedAt`) | **PASS** | Write succeeds and upgrades `updatedAt` to Firestore `Timestamp`. |
| 3 | Live prod bug repro (`7b45ed0f-...`, `lockedAt == null`) | **PASS** | `UNLINK_CAMPAIGN` fails with `PERMISSION_DENIED`; `SET_LOCK(NONE)` recovers sheet and unlocks edits. |
| 4 | Player + GM simultaneous independent edits | **PASS** | Player updates `fields/life`, GM updates `fields/story` concurrently with zero conflict. |
| 5 | Concurrent conflicting edit on `fields/money` | **PASS** | Stale `expectedRevision` rejected by Rules (`PERMISSION_DENIED`); no silent overwrite. |
| 6 | Duplicate operation receipt (`operations/{opId}`) | **PASS** | Re-writing same `opId` blocked (`create`-only); existing `COMMITTED` receipt readable for idempotency. |
| 7 | GM `HISTORIAN` lock + `unrecognizedFieldsPayload` check | **PASS** | `HISTORIAN` lock immediately blocks Player's pending edit; `unrecognizedFieldsPayload` confirmed rejected until Task 3 allowlists it. |

---

## 10. Validation & Edge-Case Test Matrix (Implementation Acceptance)

| Scenario | Acceptance Criteria | Suite |
|---|---|---|
| Legacy v1 sheet load & update | Monolithic doc normalized, types coerced, unknown keys preserved in `unrecognizedFieldsJson`, editable without manual intervention. | `LegacyFirestoreNormalizerTest`, `SparkWireCodecTest` |
| Repeated migrations | `migrate(migrate(migrate(r))) == migrate(r)`; `NeedsReview` & `unrecognizedFieldsJson` untouched; `version > CURRENT` throws `UnsupportedSchemaVersion`. | `CharacterMigrationManagerTest`, `AppDatabaseMigrationInstrumentedTest` |
| Offline edit + remote change (different fields) | Both edits converge automatically without creating a `SheetConflictRecord`. | `SparkFirestoreSyncEngineTest`, `SparkEmulatorTwoAccountInstrumentedTest` |
| Concurrent edits (same field) | Stale revision rejected -> `SheetConflictRecord` with accurate 3-way base/local/remote payloads; Keep Local / Keep Remote / Custom converge. | `SparkSyncDomainTest`, `SparkFirestoreSyncEngineTest` |
| Player + GM simultaneous edits | Independent fields & bounded deltas auto-converge; conflicting same-field writes surface explicit conflict. | `SparkEmulatorTwoAccountInstrumentedTest` |
| Connection loss / duplicate sync / partial batch failure | Mid-flight drop recovers via `operations/{opId}` idempotency check; `drainMutex` serializes duplicates; failed chunk pauses dependents (`PAUSED_DEPENDENCY`). | `SparkFirestoreSyncEngineTest` |
| Permission / lock change while pending | `PERMISSION_DENIED` marks intent `ACCESS_DENIED`, preserves draft, stops retry loop; succeeds after permission restored. | `SparkFirestoreSyncEngineTest`, `firebase/tests/firestore.rules.test.mjs` |
| App upgrade introducing new schema version | Room `35 -> 36` preserves pending `sheet_intents` & `account_sheets`; forward-version docs fail safely read-only. | `AppDatabaseMigrationInstrumentedTest`, `CharacterMigrationManagerTest` |

---

## 11. Ordered Implementation Tasks & Dependencies

1. **Task 1 — Production Data Hygiene (Pre-flight)** *(Depends on: Owner approval)*
   - Resolve `lockedAt == null` on `characters/7b45ed0f-8827-429d-ade0-66aaeaf7e1ee` (via GM `SET_LOCK(NONE)` or admin backfill).
2. **Task 2 — Canonical Domain & Sync Models (`:core:domain`)** *(Depends on: None)*
   - Add `unrecognizedFieldsJson` to `Character`; formalize delta-rebase & no-op conflict classifiers in `SparkSyncModels.kt`.
3. **Task 3 — Firebase Security Rules (`firebase/firestore.rules`)** *(Depends on: Task 2)*
   - Allowlist `unrecognizedFieldsPayload` in `validFieldValue` and re-run `npm run test:rules` + local mirror suite.
4. **Task 4 — Room Cleanup & Migration `35 -> 36` (`:app/data/local`)** *(Depends on: Task 2)*
   - Add `unrecognizedFieldsJson` to `CharacterRecord`, strip Firestore annotations, drop `sheet_routes`/`sheet_outbox`/`sheet_sequences`, add `MIGRATION_35_36`.
5. **Task 5 — Legacy Normalizer & Wire Codec Consolidation (`:app/data/sync`)** *(Depends on: Tasks 2, 4)*
   - Implement `LegacyFirestoreNormalizer`, hook into `FirestoreSparkRemoteGateway.fetchSheetDocuments` for `protocolVersion < 2`, round-trip `unrecognizedFieldsPayload` in `SparkWireCodec`, and consolidate onto `kotlinx.serialization`.
6. **Task 6 — Sync Engine Hardening (`:app/data/sync`)** *(Depends on: Tasks 2, 4, 5)*
   - Wire delta-rebase, identical-value no-op resolution, `PAUSED_DEPENDENCY` chaining, and `ACCESS_DENIED` draft recovery; remove legacy timestamp-LWW in `CharacterSyncConflict.kt`.
7. **Task 7 — Full Validation Suite** *(Depends on: Tasks 3–6)*
   - Implement and run all JVM unit tests, Room migration tests, Firebase Rules tests, and two-account local-mirror emulator tests.
