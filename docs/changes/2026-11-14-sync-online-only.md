# BACKLOG-006 / SYNC-PLAN-001 — Real-Time Sync and Online-Only Editing

- **Status**: `InDevReview`
- **Date**: 2026-11-14 (America/La_Paz)
- **Scope**: Implement `SYNC-PLAN-001-A`..`F` across the app while keeping local demo mode (`demo-player`) functional.

## Decisions & Implementation
1. **Connectivity monitoring (`SYNC-PLAN-001-D`, `E`, `F`)**
   - Added `ACCESS_NETWORK_STATE` permission in [AndroidManifest.xml](../../app/src/main/AndroidManifest.xml).
   - Added [ConnectivityMonitor.kt](../../app/src/main/java/com/kinderman/sdo/domain/repository/ConnectivityMonitor.kt) and [AndroidConnectivityMonitor.kt](../../app/src/main/java/com/kinderman/sdo/data/sync/AndroidConnectivityMonitor.kt) using `ConnectivityManager.registerDefaultNetworkCallback` (`NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`).
2. **Offline mutation blocking & read-only mode (`SYNC-PLAN-001-D`, `F`)**
   - [AppViewModel.kt](../../app/src/main/java/com/kinderman/sdo/presentation/AppViewModel.kt) exposes `isOnline`, `isOfflineReadOnly`, and `syncFailure`, blocking all authenticated mutations when offline (`save`, `autosave`, `closeCharacter`, `applySessionCommand`, `runAction`, `saveAlertSettings`, invites, and `resolveConflict`) without creating or queuing offline edits.
   - [SdoApp.kt](../../app/src/main/java/com/kinderman/sdo/presentation/SdoApp.kt) applies `readOnly = archived || offlineReadOnly` on `CharacterSheetScreen` and `SessionModeScreen` and renders `SyncStatusBanner` with reconnect/retry actions.
3. **Real-time propagation (`SYNC-PLAN-001-A`)**
   - [SyncedCharacterRepository.kt](../../app/src/main/java/com/kinderman/sdo/data/repository/SyncedCharacterRepository.kt) implements `observeRemoteChanges` with Firestore snapshot listeners on `characters`, `campaignMembers`, `campaignDeliveries`, `campaignLibrary`, and `campaignAudit`; `AppViewModel` debounces 300ms to run `startSync(initial = false)`, reduces edit idle debounce to `1_200ms`, and auto-syncs upon reconnect.
4. **Same-account convergence vs cross-account conflicts (`SYNC-PLAN-001-B`, `C`)**
   - Added `lastEditedBy` to [Character.kt](../../app/src/main/java/com/kinderman/sdo/domain/model/Character.kt), [CharacterRecord.kt](../../app/src/main/java/com/kinderman/sdo/data/local/CharacterRecord.kt), and `MIGRATION_28_29` (`version = 29`) in [AppDatabase.kt](../../app/src/main/java/com/kinderman/sdo/data/local/AppDatabase.kt).
   - `automaticRemoteFieldIds(conflict, sessionUid)` in [CharacterSyncConflict.kt](../../app/src/main/java/com/kinderman/sdo/domain/model/CharacterSyncConflict.kt) auto-converges same-account edits regardless of field count, reserving manual resolution strictly for concurrent cross-account edits.
