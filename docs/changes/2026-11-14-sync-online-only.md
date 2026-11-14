# BACKLOG-006 / SYNC-PLAN-001 — Sincronização em tempo real e uso online obrigatório

- **Status**: `InDevReview`
- **Data**: 2026-11-14 (America/La_Paz)
- **Escopo**: Implementar `SYNC-PLAN-001-A`..`F` em todo o aplicativo mantendo compatibilidade com o modo local de demonstração (`demo-player`).

## Decisões e Implementação
1. **Monitoramento de conectividade (`SYNC-PLAN-001-D`, `E`, `F`)**
   - Adicionada permissão `ACCESS_NETWORK_STATE` em [AndroidManifest.xml](../../app/src/main/AndroidManifest.xml).
   - Criada interface [ConnectivityMonitor.kt](../../app/src/main/java/com/kinderman/sdo/domain/repository/ConnectivityMonitor.kt) e implementação [AndroidConnectivityMonitor.kt](../../app/src/main/java/com/kinderman/sdo/data/sync/AndroidConnectivityMonitor.kt) via `ConnectivityManager.registerDefaultNetworkCallback` (`NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`).
2. **Bloqueio de mutações offline e modo somente leitura (`SYNC-PLAN-001-D`, `F`)**
   - [AppViewModel.kt](../../app/src/main/java/com/kinderman/sdo/presentation/AppViewModel.kt) expõe `isOnline`, `isOfflineReadOnly` e `syncFailure`, bloqueando todas as mutações autenticadas quando offline (`save`, `autosave`, `closeCharacter`, `applySessionCommand`, `runAction`, `saveAlertSettings`, convites e `resolveConflict`) sem criar ou enfileirar edições offline para envio posterior.
   - [SdoApp.kt](../../app/src/main/java/com/kinderman/sdo/presentation/SdoApp.kt) aplica `readOnly = archived || offlineReadOnly` em `CharacterSheetScreen` e `SessionModeScreen` e exibe `SyncStatusBanner` com ação de reconexão/tentativa.
3. **Propagação em tempo real (`SYNC-PLAN-001-A`)**
   - [SyncedCharacterRepository.kt](../../app/src/main/java/com/kinderman/sdo/data/repository/SyncedCharacterRepository.kt) implementa `observeRemoteChanges` com listeners Firestore em `characters`, `campaignMembers`, `campaignDeliveries`, `campaignLibrary` e `sessionOperations`; `AppViewModel` aplica debounce de 300ms e executa `startSync(initial = false)`, além de reduzir o idle debounce de edição para `1_200ms` e auto-sincronizar ao reconectar.
4. **Convergência da mesma conta vs. conflitos entre contas (`SYNC-PLAN-001-B`, `C`)**
   - Adicionado campo `lastEditedBy` em [Character.kt](../../app/src/main/java/com/kinderman/sdo/domain/model/Character.kt), [CharacterRecord.kt](../../app/src/main/java/com/kinderman/sdo/data/local/CharacterRecord.kt) e `MIGRATION_28_29` (`version = 29`) em [AppDatabase.kt](../../app/src/main/java/com/kinderman/sdo/data/local/AppDatabase.kt).
   - `automaticRemoteFieldIds(conflict, sessionUid)` em [CharacterSyncConflict.kt](../../app/src/main/java/com/kinderman/sdo/domain/model/CharacterSyncConflict.kt) converge automaticamente edições da mesma conta independentemente do número de campos, reservando diálogo manual apenas para edições concorrentes de contas diferentes.
