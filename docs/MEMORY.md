# Project Memory & Task History (`MEMORY.md`)

Centralizes active task states (`InProgress` / `InDevReview`), durable project invariants, and concise delivery history. Never create separate history/task files. Detailed commit history lives in `git log`.

## Active & Recent Deliveries (`InDevReview`)

### Compose previews (InDevReview)

Updated: 2026-11-23 22:22 America/La_Paz.
- Added Android Studio previews for presentation screens and shared controls, including light and dark theme previews where applicable.
- Dev Debug assembly passed. Android Studio preview rendering remains unverified.

### Admin field-update `ACCESS_DENIED` fixes, condition `summary` unification & admin Rules ACLs (`BACKLOG-024`, `InDevReview`)
Updated: 2026-11-20 14:25 America/La_Paz on `main`.
- **Nested-`null` `baseValue` & partial-convergence receipt fixes (`SparkFirestoreSyncEngine`, `SparkWireCodec`)**:
  - Fixed `PERMISSION_DENIED` (`SheetIntentStatus.ACCESS_DENIED`) on updates to 424 migrated production item fields containing nested `null` map entries (`abil_*`, `inv_*`, `cond_*`, `istate_*`) by preserving raw Firestore `remoteDoc["value"]` verbatim in `operations/{opId}.targets[].baseValue` and serializing wire JSON with `serializeNulls()`.
  - Fixed partial-convergence `PERMISSION_DENIED` in multi-target intents by recording only actually written targets (`writtenTargets`) in `operations/{opId}.targets` while advancing local revisions for converged targets.
- **Condition `summary` unification & zombie-tombstone prevention (`SparkWireCodec`, `CharacterRecord`, `SparkSyncModels`, `SparkSyncHelpers`)**:
  - Stored user condition notes directly in `cond_<id>/fields/summary` (eliminating duplicate 6-target `lcond_<id>` creation/tombstone cycles), preserved condition `summary` across `fromSparkDocuments -> toDomain()`, and ignored orphaned `lcond_<id>` items whose canonical `cond_<id>` is tombstoned.
- **Admin Rules & repository ACL alignment (`firebase/firestore.rules`, `SyncedCampaignRepository`, `SparkFirestoreSyncEngine`)**:
  - Allowed verified administrator (`isAdministrator()`) on `TRANSFER_OWNERSHIP`, `LINK_CAMPAIGN`, `UNLINK_CAMPAIGN`, standalone `campaignAudit` reads, `applySessionCommand` GM overrides, full campaign discovery in `SyncedCampaignRepository.sync`, and root updates preserving legacy `lockedAt == null` (`keepsRootLock`).
- **Evidence & limitations**: `:core:domain:test`, `:app:testDevDebugUnitTest` (187 tests), `:app:assembleDevDebug`, `npm --prefix firebase run test:rules` (63/63), and `.agents/gates/validate-harness.ps1` passed; updated `firebase/firestore.rules` requires push/deploy to `sdo-companion` before live cloud Rules changes take effect, and physical-device runtime verification is owned by developer.

### Sync debouncing, commit-on-blur fields, intent coalescing & conflict resolution fixes
Updated: 2026-11-20 12:01 America/La_Paz on `main`.
- **Commit-on-blur sheet inputs (`CollaborativeSheetField`, `SheetTextField`, `IntegerField`, `CharacterSheetScreen`, sheet sections)**:
  - Active sheet text and numeric inputs buffer typing locally while focused and commit edits only on focus loss, IME `Done`, disposal, or synchronous `SheetFieldCommitRegistry.flushAll()` before `Save`/`Back`/`Undo`/surface switches (while Creation Wizard preserves live draft validation and holds network sync until blur).
- **Focus-aware debouncing & outbox coalescing (`AppViewModel`, `SparkFirestoreSyncEngine`, `SparkSyncHelpers`, `SparkSyncContracts`)**:
  - Replaced the 120ms single-job sync trigger with focus-gated `pendingAutosaves`, per-character `1,200ms` idle sync debouncing, serialized `characterSyncJobs` state tracking, `800ms` debounced `schedule(immediate = false)` on saves, `ExistingWorkPolicy.REPLACE` WorkManager scheduling, and `coalesceOrChainBatches` so repeated `PENDING` `PATCH_FIELDS` edits collapse into one transaction (and reverted edits drop out).
  - Eliminated redundant full-sheet downloads by checking `SparkSheetHead.latestOperationId` against bounded LRU `recentLocalOperationIds` and using `refreshDiscoveredSheet` on initial sheet open.
- **Conflict resolution & race fixes (`SparkSyncModels`, `SparkFirestoreSyncEngine`, `SparkSyncHelpers`)**:
  - Collected all target conflicts in multi-target intents instead of aborting on the first target, preserved root `OperationType`/`TargetOp` on `TargetKind.SHEET` conflicts, converged idempotent `TargetKind.ITEM` `CREATE` targets, topologically ordered intents during reprojection, transitively rebased/unpaused dependent chains for both `useRemote = true` and `useRemote = false`, and added monotonic per-target remote snapshot merging (`mergeRemoteSnapshotMonotonically`).
- **Evidence & limitations**: `:core:domain:test`, `:app:testDevDebugUnitTest` (186 tests), `:app:assembleDevDebug`, and `.agents/gates/validate-harness.ps1` passed; physical-device runtime verification owned by developer.

### Canonical schema, legacy v1 normalization, Rules auto-deploy & conflict fixes
Updated: 2026-11-20 02:35 America/La_Paz on `main`.
- **Canonical unknown-field preservation & Room v36**: added `unrecognizedFieldsJson` to `Character` / `CharacterRecord` (`MIGRATION_35_36`, `36.json`), wired `unrecognizedFieldsPayload` in `SparkWireCodec` and `firebase/firestore.rules`, and deprecated legacy timestamp-LWW `automaticRemoteFieldIds`.
- **Legacy v1 normalization (`LegacyFirestoreNormalizer`)**: normalizes unmigrated monolithic v1 Firestore docs (`protocolVersion < 2`), coerces mismatched types/timestamps, maps deprecated properties (`notes`, `"default"` `campaignId`, PT-BR `ItemQuality` labels), and runs idempotent `CharacterMigrationManager` steps.
- **Sync & condition conflict fixes (`SparkFirestoreSyncEngine`, `SparkWireCodec`, `SparkSyncModels`, `SdoApp`)**:
  - Removed forced `appViewModel.sync()` on switching to the `Mestre` (`HISTORIAN`) tab in `SdoApp`.
  - Prevented stale in-flight `refreshSheetFromRemote` fetches from regressing locally confirmed revisions (fixing self-edit conflict popups).
  - Converged stale-revision edits whose value already matches remote (`identicalRemoteValueConvergesWithoutConflict`).
  - Fixed `cond_`/`lcond_` conflict loop by writing only `payload` on `cond_` updates and synchronizing `conditionInstances`/`conditions` projections during optimistic replay.
- **CI Firebase Rules deploy (`.github/workflows/firebase-rules.yml`)**: enabled automatic `Deploy Firestore production` on pushes to `main` touching `firebase/**`, `firebase.json`, or the workflow.
- **Production audit note**: sheet `characters/7b45ed0f-8827-429d-ade0-66aaeaf7e1ee` has `lockType = "PLAYER"` with `lockedAt = null`; root-level ops (`UNLINK_CAMPAIGN`, `DELETE_SHEET`, etc.) require `SET_LOCK(NONE)` or admin backfill first.

### UI polish, non-square avatar crop, atomic rules library & prod sheet normalization
Updated: 2026-11-19 America/La_Paz (PRs #159, #160).
- **Avatar & UI**: in-app 1:1 `AvatarCropDialog` for non-square photos (`<= 64 KiB` thumbnail), clickable Transport/Familiar preset cards, confirmed History delete, unified full-screen `InventoryItemModal` (Creation + Sheet), searchable `ChoiceField` dialog (`options > 8`), and single top bar across bottom-nav tabs.
- **Library (`player_handbook_rules.json`)**: adopted curated 1,853 atomic rules across 185 topics (`GameRulesGuide.kt`) with `#KEYWORD` and theme filters.
- **Prod sheets**: normalized all 10 active production sheets in `sdo-companion` against `BuiltInCatalog` and `SparkWireCodec` with zero post-migration diff.

## Durable Project Invariants

- **Architecture**: `:core:domain` (pure Kotlin/JVM, zero Android/Firebase deps) + `:app` (Compose, Hilt 2.60.1, Room v36 `exportSchema = true`, custom `AppSurface` navigation in `SdoApp`).
- **Design system**: presentation screens must use `Sdo*` controls (`Standard` & `Cybergrunge` modes); CI blocks raw Material3 `Button`/`Card`/`Switch`/etc. imports in `presentation/`.
- **Spark Firestore Protocol v2 (`protocolVersion = 2`, `schemaVersion = 2`)**: 100% Firebase Spark (no Cloud Functions, Cloud Storage, or RTDB sheet mirrors). Decomposed `characters/{id}` + `fields/` + `items/` + `items/{id}/fields/` + immutable `operations/{opId}` receipts (`<=10` Rules accesses/rule, `<=20`/tx), `presence/` (`<=120s` TTL), and `media/avatar` (`<=64 KiB`, `<=256px`). Isolated RTDB retains only `grid/{campaignId}/{gridId}`.

## Completed Milestones (`Done`)

| Date | Milestone | Summary |
|---|---|---|
| 2026-11-18 | `BACKLOG-014..021` | Sync icon rotation, styled read-only Path/Notes, Powers/Mystic & Session/GM UI upgrades, spoiler-free lore catalog expansion. |
| 2026-11-17 | `:core:domain` & Hilt | Extracted `:core:domain`, moved 22k-line catalog Kotlin to JSON resource, enabled Hilt DI and Room schema export. |
| 2026-11-16 | `BACKLOG-001..004, 006..013` | Shared campaign journal, compact Session/GM quick actions, profile avatars, Spark Firestore Protocol v2 cutover & Room v33 outbox. |
