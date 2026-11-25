# Project Memory & Task History (`MEMORY.md`)

Centralizes active task states (`InProgress` / `InDevReview`), durable project invariants, and concise delivery history. Never create separate history/task files. Detailed commit history lives in `git log`.

## Active & Recent Deliveries (`InDevReview`)

### Deity review corrections, Presence capacity, weakening and growth (`BACKLOG-026`, `InDevReview`)
Updated: 2026-11-24 20:41 America/La_Paz on `lab/deity-system-library-inventory-fixes` (PR #164).
- Owner authorized completing the numerical rules on 2026-11-24. Fixed inventory stack quantity/load, full structured GM detail, ascension budgets and Destiny refund, Avatar spending and secured actual patron Presence, and Corruption channeling restrictions.
- Permanent Pillars and current strength are separate. Shared capacity is twice current Presence; Anchors cost 1 and Avatars cost 2. Overload is limited to 2, costs 1 PD per excess point per scene, and suspends newest unaffordable links without debt. Scene IDs prevent duplicate charges.
- Vulnerability suppression is 1 or 2 points, combined cap 2 per Pillar. Scene-only suppression expires on scene reset. Persistent suppression recovers 1 per confirmed restorative scene, at most once per session. Permanent growth/loss requires unique milestones and arc IDs, within category caps. Profile edits preserve permanent Pillars.
- Integrated divine UI & handbook updates per owner feedback:
  - Added canonical `Deidades e Avatares` topics + reconciled `Corrupção Divina` resistances in `player_handbook_rules.json` (`1,893` atomic rules across `192` topics, `Deidades & Divino` category, `#DEIDADES` keyword).
  - Removed standalone `18 DEIDADES` sheet tab: embedded a pink (`#EE227D`) `Ascensão Divina` card inside `01—03 PERFIL` (editable only by GM/Admin and hidden from players while `Mortal`), moved `Marcas divinas` into `13 ESTADO` alongside `Condições`, wired active & passive divine capabilities as `PD`-backed Powers in `08 PODERES` (syncing passive capabilities on ascension and tracking scene PD/Tabu/channeling in Session Mode), and included same-campaign Lesser/Greater God sheets alongside catalog deities in patron/mark pickers.
- Updated canonical `Deidades e Avatares.md` and `Corrupção Divina.md` in sibling `solidao-dos-oprimidos`, preserving its unrelated dirty files. Deity resistance contradictions are reconciled, Greater God reserve is 10 PD and other categories 5, and recovery rounds Sanity cost up with minimum 1.
- Sustained-channeling controls propagate into generated modifiers/conditions. Rupture ends explicitly sustained effects on the current sheet. Legacy effects without metadata remain unchanged; other sheets require their own adjudication.
- Reconciled with `main` (`cc176fe` Compose previews) on `lab/deity-system-library-inventory-fixes`, mirrored `sdo-companion` -> `sdo-companion-dev` (`10,358` docs including 16 `deityGmCatalog` records), and opened PR #164.
- Validation passed on merged branch: `:core:domain:test`, `:app:testDevDebugUnitTest`, `:app:testProdDebugUnitTest`, `:app:assembleDevDebug`, `:app:assembleDevDebugAndroidTest`, `:app:lintProdDebug`, `npm --prefix firebase run test:rules` (73/73), all four catalog generators, presentation design-system import boundary, and `.agents/gates/validate-harness.ps1`. Physical-device runtime verification and Compose preview rendering remain unverified.

### Deity system, compact Library/Inventory filters & inventory/character-creation fixes (`BACKLOG-025`, `InDevReview`)
Updated: 2026-11-23 23:03 America/La_Paz on `lab/deity-system-library-inventory-fixes`.
- **Owner rule confirmation**: **Greater Gods (`Deus Maior`) have a base Destiny limit (`destinyBaseCeiling`) of `10 PD`** (implemented in `DivineSystem.kt:438`; Mortal/Avatar/Demigod/Lesser God base ceiling remains `5 PD`, plus `destiny.adjustment` + modifiers).
- **Completed (Pipeline, Domain, Data/Persistence Room v37, Sync & Firestore Rules)**:
  1. **Vault Deity Pipeline (`tools/generate_deity_catalog.py`, `tools/generate_catalog.py`, `.github/workflows/sdo-companion-android.yml`)**:
     - Imported all 16 deity sheets + `Deidades.md` index from `..\solidao-dos-oprimidos` (12 Lesser/Greater Gods: 3 `Deus Maior` + 9 `Deus Menor`; 3 `Semideus`/`Semideusa`; 1 `Avatar`).
     - Enforced strict data-layer spoiler separation: `catalogs/deities.json` -> `built_in_catalog.json` (`BuiltInCatalog.VERSION = 4`, `CatalogKind.DEITY`) contains **only** `Para Jogadores (O Que Se Sabe)` and public identity; GM-only content (`Para o Mestre`, Postura/Tabu, Pillars/Stats, Domain Acts, Passives, Herald Gifts, Contracts, Behavior, Table Hooks, Central Mysteries) is generated into `firebase/deity_gm_catalog.json` + `firebase/scripts/seed_deity_secrets.mjs` outside APK bundled assets. All 5 Python catalog checks pass.
  2. **Domain Rules & Models (`:core:domain`)**:
     - `DivineSystem.kt`: `DivineScale`, `DivineTier` (`MORTAL`, `AVATAR`, `DEMIGOD`, `LESSER_GOD`, `GREATER_GOD`, unplayable `PRIMORDIAL`), `DivinePillars` (0–5), `DivineMark` (`arauto`/`marcado`, `Free`/`Reaction`/`Passive`/`Action`, living sensory Anchor), `CharacterDivineAscension`, `CharacterDivineRuntime`, `DivineRules` (`CD = 15 + Pilar + PD extras`, deity clashes with Santuário & tier tie-breakers, Tabu `-2 PD` + scene Domain Act block + `resetDivineScene`, `10%` PS/Anchor -> `+1 PD`, patron/tier `calculateCorruptionDelta`, single authoritative `deriveEffects` with `10 PD` Greater God ceiling, Darno runes + `Eco Emocional`, mental immunity, Material Restriction).
     - `DeityAccessPolicy.kt` & `DeityRepository.kt`: campaign-context GM (`campaign.ownerId == session.uid` on non-deleted campaign) + verified Admin authorization; rejects `UserRole.MASTER`, `CampaignRole.HISTORIAN`, Historian UI surface without campaign ownership, and non-owned campaign contexts.
     - `ItemCategory.kt`, `ItemCreation.kt`, `ItemCreationRules.kt`, `ItemNormalization.kt`, `Character.kt`: canonical item category mapper fixing `Arco ou besta` -> `Armas` (`WIELDED`), `Escudo` -> `Armaduras` (`WIELDED`/`EQUIPPED`), explicit handling of `LEGACY_NARRATIVE`, blank, and custom categories, plus atomic `Character.addCatalogInventoryItems` batch helper.
     - `CharacterAccessPolicy.kt`, `CharacterSyncConflict.kt`, `SparkSyncModels.kt`: wired `divineAscensionPayload` & `divineRuntimePayload` and `canChangeAscension`.
  3. **Data, Room v37, Sync & Firestore Rules (`:app`, `firebase/firestore.rules`)**:
     - `AppDatabase.kt` (`version = 37`, `MIGRATION_36_37`), `DeityGmCacheRecord.kt`, `CharacterRecord.kt`, `CharacterConverters.kt`, `DatabaseModule.kt`, `RepositoryModule.kt`.
     - `SecuredDeityRepository.kt` & `FirestoreDeityGmRemoteDataSource`: enforces `DeityAccessPolicy` before any remote/cache read, isolates GM offline cache by `(viewerUid, campaignScopeId, deityId)`, purges on logout/session switch.
     - `SparkWireCodec.kt`, `LegacyFirestoreNormalizer.kt`, `SparkFirestoreSyncEngine.kt`: syncs divine fields and enforces GM/Admin write ACL on `divineAscension` mutations.
     - `firebase/firestore.rules`: allowlisted `divineAscensionPayload`/`divineRuntimePayload`, locked `divineAscensionPayload` writes to `isAdministrator() || isActiveGm`, and added `/gmActiveContext/{uid}`, `/deityGmCatalog/{deityId}`, and `/campaigns/{campaignId}/deitySecrets/{deityId}` rules.
- **Compose delivery**. Library, Historian, and inventory use an accessible collapsible filter control with an active summary and a clear action. The catalog picker preserves multiple selections across search and sorting and applies them as a single batch. The custom builder persists its draft independently. The character sheet adds a Deidades page with divine controls and an Ascension editor restricted to campaign owners and verified administrators.
- **Spoiler isolation**. Library and Historian use repository authorization and clear dialog state when campaign or viewer permissions change. Character-sheet references load public details. The app purges other viewers' offline cache data on session changes. Firestore limits global GM reads to a recent authorized campaign context, and the client clears that context after a fetch. Ascension authorization runs on its specific field write to preserve the operation expression budget.
- **Evidence**. Dev Debug assembly passed. Domain tests passed (194), both Android unit suites passed (189 each), and Firebase emulator Rules tests passed (65). Prod Debug lint, all four catalog generators, the presentation import check, and the harness gate passed. Galaxy S24 Ultra emulator smoke verified the Deidades filter returns 16 records and local player details show public lore. Authenticated GM UI, divine sheet controls, and inventory picker still need runtime acceptance.
- **Deployment**. The GM catalog still needs to be seeded in the target Firebase project and the revised Firestore Rules deployed before online GM lore is available there.

### Compose previews (`InDevReview`)
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

- **Architecture**: `:core:domain` (pure Kotlin/JVM, zero Android/Firebase deps) + `:app` (Compose, Hilt 2.60.1, Room v37 `exportSchema = true`, custom `AppSurface` navigation in `SdoApp`).
- **Design system**: presentation screens must use `Sdo*` controls (`Standard` & `Cybergrunge` modes); CI blocks raw Material3 `Button`/`Card`/`Switch`/etc. imports in `presentation/`.
- **Spark Firestore Protocol v2 (`protocolVersion = 2`, `schemaVersion = 2`)**: 100% Firebase Spark (no Cloud Functions, Cloud Storage, or RTDB sheet mirrors). Decomposed `characters/{id}` + `fields/` + `items/` + `items/{id}/fields/` + immutable `operations/{opId}` receipts (`<=10` Rules accesses/rule, `<=20`/tx), `presence/` (`<=120s` TTL), and `media/avatar` (`<=64 KiB`, `<=256px`). Isolated RTDB retains only `grid/{campaignId}/{gridId}`.

## Completed Milestones (`Done`)

| Date | Milestone | Summary |
|---|---|---|
| 2026-11-18 | `BACKLOG-014..021` | Sync icon rotation, styled read-only Path/Notes, Powers/Mystic & Session/GM UI upgrades, spoiler-free lore catalog expansion. |
| 2026-11-17 | `:core:domain` & Hilt | Extracted `:core:domain`, moved 22k-line catalog Kotlin to JSON resource, enabled Hilt DI and Room schema export. |
| 2026-11-16 | `BACKLOG-001..004, 006..013` | Shared campaign journal, compact Session/GM quick actions, profile avatars, Spark Firestore Protocol v2 cutover & Room v33 outbox. |
