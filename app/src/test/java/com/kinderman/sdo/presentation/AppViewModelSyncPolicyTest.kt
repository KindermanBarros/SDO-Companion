package com.kinderman.sdo.presentation

import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignAlertSettings
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignInvite
import com.kinderman.sdo.domain.model.CampaignInvitePreview
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.ConflictResolutionStatus
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.toSyncStatus
import com.kinderman.sdo.domain.repository.CampaignRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.ConnectivityMonitor
import com.kinderman.sdo.domain.repository.OperationsRepository
import com.kinderman.sdo.domain.repository.OwnerRepository
import com.kinderman.sdo.domain.repository.RemoteSyncEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppViewModelSyncPolicyTest {
    private val session = UserSession("user-1", "user@sdo.test", "Jogador", UserRole.USER)
    private lateinit var testScope: CoroutineScope
    private lateinit var connectivity: FakeConnectivityMonitor
    private lateinit var characterRepo: FakeCharacterRepository
    private lateinit var campaignRepo: FakeCampaignRepository
    private lateinit var operationsRepo: FakeOperationsRepository
    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        connectivity = FakeConnectivityMonitor(initialOnline = true)
        characterRepo = FakeCharacterRepository()
        campaignRepo = FakeCampaignRepository()
        operationsRepo = FakeOperationsRepository()
        viewModel = AppViewModel(
            repository = characterRepo,
            ownerRepository = FakeOwnerRepository(),
            catalogRepository = FakeCatalogRepository(),
            campaignRepository = campaignRepo,
            operationsRepository = operationsRepo,
            connectivityMonitor = connectivity,
            externalScope = testScope,
        )
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun migratedSheetPersistsOfflineWhileCampaignAdministrationRemainsBlocked() = runBlocking {
        characterRepo.offlineIds.value = setOf("char-1")
        viewModel.setSession(session)
        viewModel.offlineEditableIds.first { "char-1" in it }
        connectivity.onlineState.value = false
        val character = Character(id = "char-1", ownerId = session.uid, name = "Aria")

        viewModel.autosave(character)
        viewModel.applySessionCommand(character, SessionCommand(type = SessionOperationType.MONEY, amount = 5))
        viewModel.createCampaign("Offline", "")

        assertEquals(1, characterRepo.saveCount)
        assertEquals(1, operationsRepo.applyCount)
        assertEquals(0, campaignRepo.createCount)
    }

    @Test
    fun offlineAuthenticatedSessionBlocksMutationsWithoutQueuingEdits() = runBlocking {
        viewModel.setSession(session)
        connectivity.onlineState.value = false

        assertTrue(viewModel.isOfflineReadOnly.first { it })

        val character = Character(id = "char-1", ownerId = session.uid, name = "Aria")
        viewModel.save(character)
        viewModel.autosave(character)
        viewModel.createCampaign("Campanha Offline", "")
        viewModel.applySessionCommand(character, SessionCommand(type = SessionOperationType.MONEY, amount = 5))
        viewModel.delete(character)

        assertEquals(0, characterRepo.saveCount)
        assertEquals(0, characterRepo.deleteCount)
        assertEquals(0, campaignRepo.createCount)
        assertEquals(0, operationsRepo.applyCount)
        assertEquals(AppViewModel.OFFLINE_READ_ONLY_MESSAGE, viewModel.message.value)
    }

    @Test
    fun reconnectingRestoresEditingAndTriggersSync() = runBlocking {
        connectivity.onlineState.value = false
        viewModel.setSession(session)
        assertTrue(viewModel.isOfflineReadOnly.first { it })
        assertEquals(0, characterRepo.syncCount)

        connectivity.onlineState.value = true
        assertFalse(viewModel.isOfflineReadOnly.first { !it })
        assertTrue(characterRepo.syncCount >= 1)

        viewModel.save(Character(id = "char-1", ownerId = session.uid, name = "Aria"))
        assertEquals(1, characterRepo.saveCount)
    }

    @Test
    fun remoteChangeSignalTriggersRealtimeSyncWhenOnline() = runBlocking {
        viewModel.setSession(session)
        val initialSyncs = characterRepo.syncCount

        characterRepo.remoteChanges.emit(RemoteSyncEvent(fromCloud = true, fullSync = true))
        delay(450)

        assertTrue(characterRepo.syncCount > initialSyncs)
    }

    @Test
    fun demoSessionRemainsEditableOffline() = runBlocking {
        connectivity.onlineState.value = false
        viewModel.setDemoSession()

        assertFalse(viewModel.isOfflineReadOnly.value)
        viewModel.save(Character(id = "char-demo", ownerId = AppViewModel.DEMO_USER_ID, name = "Demo"))

        assertEquals(1, characterRepo.saveCount)
    }

    @Test
    fun fieldPresenceIgnoresBlankIdAndSwallowsUpstreamPermissionErrors() = runBlocking {
        viewModel.setSession(session)

        assertEquals(emptyMap<String, String>(), viewModel.observeFieldPresence("").first())
        assertEquals(0, characterRepo.presenceCalls)

        characterRepo.throwOnPresence = true
        assertEquals(emptyMap<String, String>(), viewModel.observeFieldPresence("char-1").first())
        assertEquals(1, characterRepo.presenceCalls)
    }

    @Test
    fun deleteAndSaveTriggerSelectiveCharacterSyncInsteadOfFullSync() = runBlocking {
        viewModel.setSession(session)
        val initialFullSyncs = characterRepo.syncCount
        val character = Character(id = "char-selective", ownerId = session.uid, name = "Kael")

        viewModel.delete(character)
        delay(50)

        assertEquals(1, characterRepo.deleteCount)
        assertEquals(listOf("char-selective"), characterRepo.syncedCharacterIds)
        assertEquals(initialFullSyncs, characterRepo.syncCount)
    }

    @Test
    fun localEditsManualSyncAndUploadAcksDoNotTriggerCloudSyncingAnimation() = runBlocking {
        val observedCloudSyncStates = mutableListOf<Boolean>()
        val vm = AppViewModel(
            repository = characterRepo,
            ownerRepository = FakeOwnerRepository(),
            catalogRepository = FakeCatalogRepository(),
            campaignRepository = campaignRepo,
            operationsRepository = operationsRepo,
            connectivityMonitor = connectivity,
            externalScope = testScope,
            cloudSyncPulseMs = 0L,
        )
        vm.setSession(session)
        observedCloudSyncStates += vm.loadState.value.cloudSyncing

        vm.save(Character(id = "char-1", ownerId = session.uid, name = "Aria"))
        observedCloudSyncStates += vm.loadState.value.cloudSyncing

        vm.sync()
        observedCloudSyncStates += vm.loadState.value.cloudSyncing

        // Simulated upload acknowledgement / unchanged snapshot
        characterRepo.remoteChanges.emit(RemoteSyncEvent(fromCloud = false, characterIds = setOf("char-1"), fullSync = false))
        delay(50)
        observedCloudSyncStates += vm.loadState.value.cloudSyncing

        assertTrue(observedCloudSyncStates.none { it })
    }

    @Test
    fun committedCloudChangeTriggersCloudSyncingAndSelectiveCharacterSync() = runBlocking {
        var cloudSyncingDuringFetch = false
        val vm = AppViewModel(
            repository = characterRepo,
            ownerRepository = FakeOwnerRepository(),
            catalogRepository = FakeCatalogRepository(),
            campaignRepository = campaignRepo,
            operationsRepository = operationsRepo,
            connectivityMonitor = connectivity,
            externalScope = testScope,
            cloudSyncPulseMs = 0L,
        )
        vm.setSession(session)
        val initialFullSyncs = characterRepo.syncCount
        characterRepo.onSyncCharacter = { _ ->
            cloudSyncingDuringFetch = vm.loadState.value.cloudSyncing
        }

        characterRepo.remoteChanges.emit(RemoteSyncEvent(fromCloud = true, characterIds = setOf("char-remote"), fullSync = false))
        delay(50)

        assertTrue(cloudSyncingDuringFetch)
        assertFalse(vm.loadState.value.cloudSyncing)
        assertEquals(listOf("char-remote"), characterRepo.syncedCharacterIds)
        assertEquals(initialFullSyncs, characterRepo.syncCount)
    }

    @Test
    fun characterSyncTogglesSyncingFlagAndSaveDoesNotEmitToastMessage() = runBlocking {
        val observedSyncing = mutableListOf<Boolean>()
        val vm = AppViewModel(
            repository = characterRepo,
            ownerRepository = FakeOwnerRepository(),
            catalogRepository = FakeCatalogRepository(),
            campaignRepository = campaignRepo,
            operationsRepository = operationsRepo,
            connectivityMonitor = connectivity,
            externalScope = testScope,
            cloudSyncPulseMs = 0L,
        )
        vm.setSession(session)
        characterRepo.onSyncCharacter = { _ ->
            observedSyncing += vm.loadState.value.syncing
        }

        vm.save(Character(id = "char-toast-free", ownerId = session.uid, name = "Vespera"))
        delay(2_500)

        assertEquals(null, vm.message.value)
        assertEquals(listOf(true), observedSyncing)
        assertFalse(vm.loadState.value.syncing)
        assertFalse(vm.loadState.value.cloudSyncing)
    }

    @Test
    fun sparkConflictResolutionDelegatesChoiceAndSyncs() = runBlocking {
        val conflict = SheetConflictItem(
            id = "conflict-1",
            accountId = session.uid,
            sheetId = "char-1",
            operationId = "op-1",
            targetKey = "field:name",
            label = "Nome",
            baseRevision = 1L,
            remoteRevision = 2L,
            baseSummary = "Aria",
            localSummary = "Aria L.",
            remoteSummary = "Aria R.",
            basePayloadJson = "\"Aria\"",
            localPayloadJson = "\"Aria L.\"",
            remotePayloadJson = "\"Aria R.\"",
        )
        characterRepo.accountConflicts[session.uid] = listOf(conflict)
        characterRepo.emitStoreUpdate()
        viewModel.setSession(session)

        assertEquals(listOf(conflict), viewModel.sheetConflicts.first { it.isNotEmpty() })
        val syncsBefore = characterRepo.syncCount

        viewModel.resolveIntentConflict(conflictId = "conflict-1", useRemote = false, customValueJson = "\"Aria Custom\"")
        delay(50)

        assertEquals(listOf(Triple("conflict-1", false, "\"Aria Custom\"")), characterRepo.resolvedIntentConflicts)
        assertTrue(viewModel.sheetConflicts.first { it.isEmpty() }.isEmpty())
        assertTrue(characterRepo.syncCount > syncsBefore)
        assertEquals("Conflito resolvido", viewModel.message.value)
    }

    @Test
    fun accessDeniedDraftCanBeRetriedOrDiscarded() = runBlocking {
        val deniedA = SheetIntent(
            operationId = "op-denied-a",
            accountId = session.uid,
            sheetId = "char-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            desiredPayloadJson = "\"Aria\"",
            status = SheetIntentStatus.ACCESS_DENIED,
            error = "Sem permissão",
        )
        val deniedB = deniedA.copy(operationId = "op-denied-b", targetKey = "field:occupation")
        characterRepo.accountIntents[session.uid] = listOf(deniedA, deniedB)
        characterRepo.emitStoreUpdate()
        viewModel.setSession(session)

        assertEquals(2, viewModel.sheetSyncStatus.first { it.accessDeniedCount == 2 }.accessDeniedCount)

        viewModel.retryAccessDeniedDraft("op-denied-a")
        delay(50)
        assertEquals(listOf("op-denied-a"), characterRepo.retriedAccessDeniedIds)
        val afterRetry = viewModel.sheetIntents.first { list ->
            list.firstOrNull { it.operationId == "op-denied-a" }?.status == SheetIntentStatus.PENDING
        }
        assertEquals(SheetIntentStatus.PENDING, afterRetry.first { it.operationId == "op-denied-a" }.status)

        viewModel.discardDraft("op-denied-b")
        delay(50)
        assertEquals(listOf("op-denied-b"), characterRepo.discardedDraftIds)
        val afterDiscard = viewModel.sheetIntents.first { list -> list.none { it.operationId == "op-denied-b" } }
        assertEquals(listOf("op-denied-a"), afterDiscard.map { it.operationId })
    }

    @Test
    fun fieldPresenceIsStrictlyGatedOnSheetSurfaceAndClearedOnExitOrBackground() = runBlocking {
        viewModel.setSession(session)

        // Not on AppSurface.SHEET: activation must be ignored.
        viewModel.setFieldPresence("char-1", "name", true)
        delay(50)
        assertTrue(characterRepo.presenceWrites.isEmpty())

        // Entering AppSurface.SHEET enables presence writes for that sheet only.
        viewModel.setSheetPresenceActive(active = true, characterId = "char-1")
        viewModel.setFieldPresence("char-1", "name", true)
        viewModel.setFieldPresence("char-other", "name", true)
        delay(50)
        assertEquals(
            listOf(Triple("char-1", "name", true)),
            characterRepo.presenceWrites,
        )

        // Leaving AppSurface.SHEET clears active presence.
        viewModel.setSheetPresenceActive(active = false, characterId = null)
        delay(50)
        assertEquals(
            listOf(
                Triple("char-1", "name", true),
                Triple("char-1", null, false),
            ),
            characterRepo.presenceWrites,
        )

        // Re-entering SHEET and backgrounding (not config change) clears active presence.
        viewModel.onAppForegrounded()
        viewModel.setSheetPresenceActive(active = true, characterId = "char-1")
        viewModel.setFieldPresence("char-1", "life", true)
        viewModel.onAppBackgrounded(changingConfiguration = true)
        delay(50)
        assertEquals(Triple("char-1", "life", true), characterRepo.presenceWrites.last())
        viewModel.onAppBackgrounded(changingConfiguration = false)
        delay(50)
        assertEquals(Triple("char-1", null, false), characterRepo.presenceWrites.last())
    }

    @Test
    fun accountSwitchAndClearSessionIsolateUiStateWithoutDiscardingDurableQueues() = runBlocking {
        val sessionB = UserSession("user-2", "other@sdo.test", "Outro Jogador", UserRole.USER)
        val intentA = SheetIntent(
            operationId = "op-a",
            accountId = session.uid,
            sheetId = "char-a",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            status = SheetIntentStatus.ACCESS_DENIED,
        )
        val conflictA = SheetConflictItem(
            id = "conf-a",
            accountId = session.uid,
            sheetId = "char-a",
            operationId = "op-a",
            targetKey = "field:name",
            label = "Nome",
            baseRevision = 1L,
            remoteRevision = 2L,
            baseSummary = "Base",
            localSummary = "Local",
            remoteSummary = "Remote",
            basePayloadJson = "\"Base\"",
            localPayloadJson = "\"Local\"",
            remotePayloadJson = "\"Remote\"",
        )
        val intentB = SheetIntent(
            operationId = "op-b",
            accountId = sessionB.uid,
            sheetId = "char-b",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:level",
            status = SheetIntentStatus.PENDING,
        )
        characterRepo.accountIntents[session.uid] = listOf(intentA)
        characterRepo.accountConflicts[session.uid] = listOf(conflictA)
        characterRepo.accountIntents[sessionB.uid] = listOf(intentB)
        characterRepo.emitStoreUpdate()

        viewModel.setSession(session)
        assertEquals(listOf(intentA), viewModel.sheetIntents.first { it == listOf(intentA) })
        assertEquals(listOf(conflictA), viewModel.sheetConflicts.first { it == listOf(conflictA) })

        viewModel.clearSession()
        assertTrue(viewModel.sheetIntents.first { it.isEmpty() }.isEmpty())
        assertTrue(viewModel.sheetConflicts.first { it.isEmpty() }.isEmpty())
        assertEquals(SheetSyncStatus(), viewModel.sheetSyncStatus.first { it == SheetSyncStatus() })
        // Durable Room queues for account A remain intact in the repository.
        assertEquals(listOf(intentA), characterRepo.accountIntents[session.uid])
        assertEquals(listOf(conflictA), characterRepo.accountConflicts[session.uid])

        // Switching to account B exposes only B's intents/conflicts.
        viewModel.setSession(sessionB)
        assertEquals(listOf(intentB), viewModel.sheetIntents.first { it == listOf(intentB) })
        assertTrue(viewModel.sheetConflicts.first { it.isEmpty() }.isEmpty())

        // Switching back to account A restores A's durable queue and conflicts.
        viewModel.setSession(session)
        assertEquals(listOf(intentA), viewModel.sheetIntents.first { it == listOf(intentA) })
        assertEquals(listOf(conflictA), viewModel.sheetConflicts.first { it == listOf(conflictA) })
    }

    @Test
    fun linkAndUnlinkCharacterDoNotTriggerRedundantRepositorySave() = runBlocking {
        viewModel.setSession(session)
        val character = Character(id = "char-1", ownerId = session.uid, name = "Aria")
        val campaign = Campaign(id = "camp-1", name = "Mesa", ownerId = session.uid)

        viewModel.linkCharacter(character, campaign)
        viewModel.unlinkCharacter(character.copy(campaignId = campaign.id))
        viewModel.acceptCampaignInvite("INV123", character, createNewCharacter = false)
        delay(50)

        assertEquals(0, characterRepo.saveCount)
    }

    @Test
    fun autosaveHoldsEditsWhileFieldFocusedAndFlushesOnceWhenFocusEnds() = runBlocking {
        val vm = AppViewModel(
            repository = characterRepo,
            ownerRepository = FakeOwnerRepository(),
            catalogRepository = FakeCatalogRepository(),
            campaignRepository = campaignRepo,
            operationsRepository = operationsRepo,
            connectivityMonitor = connectivity,
            externalScope = testScope,
            cloudSyncPulseMs = 0L,
            autosaveDebounceMs = 80L,
            editingIdleSyncDelayMs = 80L,
        )
        vm.setSession(session)
        vm.setSheetPresenceActive(active = true, characterId = "char-focus")
        vm.setFieldPresence("char-focus", "identity/name", active = true)

        // Simulate typing multiple characters with pauses longer than debounce while still focused.
        listOf("A", "Ar", "Ari", "Aria", "Aria Vespera").forEach { typed ->
            vm.autosave(Character(id = "char-focus", ownerId = session.uid, name = typed))
            delay(40)
        }
        delay(200)
        assertEquals("Zero saves should fire while field remains focused", 0, characterRepo.saveCount)
        assertTrue(characterRepo.syncedCharacterIds.isEmpty())

        // Losing focus flushes the final edited state once and triggers a single character sync.
        vm.setFieldPresence("char-focus", "identity/name", active = false)
        delay(220)
        assertEquals(1, characterRepo.saveCount)
        assertEquals("Aria Vespera", characterRepo.lastSavedCharacter?.name)
        assertEquals(listOf("char-focus"), characterRepo.syncedCharacterIds)
    }

    @Test
    fun perCharacterDebounceIsolatesSheetsAndCloseFlushesImmediately() = runBlocking {
        val vm = AppViewModel(
            repository = characterRepo,
            ownerRepository = FakeOwnerRepository(),
            catalogRepository = FakeCatalogRepository(),
            campaignRepository = campaignRepo,
            operationsRepository = operationsRepo,
            connectivityMonitor = connectivity,
            externalScope = testScope,
            cloudSyncPulseMs = 0L,
            autosaveDebounceMs = 60L,
            editingIdleSyncDelayMs = 80L,
        )
        vm.setSession(session)

        vm.autosave(Character(id = "char-a", ownerId = session.uid, name = "A1"))
        vm.autosave(Character(id = "char-a", ownerId = session.uid, name = "A2"))
        vm.autosave(Character(id = "char-b", ownerId = session.uid, name = "B1"))
        delay(250)

        assertEquals(2, characterRepo.saveCount)
        assertEquals(setOf("char-a", "char-b"), characterRepo.syncedCharacterIds.toSet())

        characterRepo.syncedCharacterIds.clear()
        vm.closeCharacter(Character(id = "char-close", ownerId = session.uid, name = "Fechada"))
        delay(30)
        assertEquals(listOf("char-close"), characterRepo.syncedCharacterIds)
    }

    private class FakeConnectivityMonitor(initialOnline: Boolean) : ConnectivityMonitor {
        val onlineState = MutableStateFlow(initialOnline)
        override val isOnline = onlineState
    }

    private class FakeCharacterRepository : CharacterRepository {
        val offlineIds = MutableStateFlow<Set<String>>(emptySet())
        override fun observeOfflineEditableIds(session: UserSession): Flow<Set<String>> = offlineIds
        var throwOnPresence = false
        var presenceCalls = 0
        val presenceWrites = mutableListOf<Triple<String, String?, Boolean>>()
        val accountIntents = mutableMapOf<String, List<SheetIntent>>()
        val accountConflicts = mutableMapOf<String, List<SheetConflictItem>>()
        private val storeRevision = MutableStateFlow(0)
        val resolvedIntentConflicts = mutableListOf<Triple<String, Boolean, String?>>()
        val retriedAccessDeniedIds = mutableListOf<String>()
        val discardedDraftIds = mutableListOf<String>()

        fun emitStoreUpdate() {
            storeRevision.value++
        }

        override fun observeSyncStatus(session: UserSession): Flow<SheetSyncStatus> =
            storeRevision.map { (accountIntents[session.uid].orEmpty()).toSyncStatus() }

        override fun observeIntents(session: UserSession): Flow<List<SheetIntent>> =
            storeRevision.map { accountIntents[session.uid].orEmpty() }

        override fun observeConflicts(session: UserSession): Flow<List<SheetConflictItem>> =
            storeRevision.map {
                accountConflicts[session.uid].orEmpty().filter { it.resolutionStatus == ConflictResolutionStatus.OPEN }
            }

        override suspend fun resolveIntentConflict(
            session: UserSession,
            conflictId: String,
            useRemote: Boolean,
            customValueJson: String?,
        ) {
            resolvedIntentConflicts += Triple(conflictId, useRemote, customValueJson)
            accountConflicts[session.uid] = accountConflicts[session.uid].orEmpty().filterNot { it.id == conflictId }
            emitStoreUpdate()
        }

        override suspend fun retryAccessDeniedDraft(session: UserSession, intentId: String) {
            retriedAccessDeniedIds += intentId
            accountIntents[session.uid] = accountIntents[session.uid].orEmpty().map { intent ->
                if (intent.operationId == intentId) {
                    intent.copy(status = SheetIntentStatus.PENDING, error = "")
                } else {
                    intent
                }
            }
            emitStoreUpdate()
        }

        override suspend fun discardDraft(session: UserSession, intentId: String) {
            discardedDraftIds += intentId
            accountIntents[session.uid] = accountIntents[session.uid].orEmpty().filterNot { it.operationId == intentId }
            accountConflicts[session.uid] = accountConflicts[session.uid].orEmpty().filterNot { it.operationId == intentId }
            emitStoreUpdate()
        }

        override suspend fun setFieldPresence(
            session: UserSession,
            characterId: String,
            fieldKey: String?,
            active: Boolean,
        ) {
            presenceWrites += Triple(characterId, fieldKey, active)
        }

        override fun observeFieldPresence(session: UserSession, characterId: String): Flow<Map<String, String>> =
            kotlinx.coroutines.flow.flow {
                presenceCalls++
                if (throwOnPresence) throw IllegalStateException("Firebase Database error: Permission denied")
                emit(emptyMap())
            }
        val remoteChanges = MutableSharedFlow<RemoteSyncEvent>(extraBufferCapacity = 4)
        var saveCount = 0
        var lastSavedCharacter: Character? = null
        var deleteCount = 0
        var syncCount = 0
        val syncedCharacterIds = mutableListOf<String>()
        var onSyncCharacter: (suspend (String) -> Unit)? = null

        override fun observe(session: UserSession): Flow<List<Character>> = flowOf(emptyList())
        override fun observeOne(id: String): Flow<Character?> = flowOf(null)
        override fun observeRemoteChanges(session: UserSession, campaignIds: Set<String>): Flow<RemoteSyncEvent> = remoteChanges
        override suspend fun create(session: UserSession, ownerId: String, campaignId: String): Character =
            Character(ownerId = ownerId, campaignId = campaignId)

        override suspend fun save(session: UserSession, character: Character) {
            saveCount++
            lastSavedCharacter = character
        }

        override suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean) = Unit
        override suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean) = Unit
        override suspend fun transferOwnership(session: UserSession, character: Character, owner: UserProfile) = Unit
        override suspend fun delete(session: UserSession, character: Character) {
            deleteCount++
        }

        override suspend fun sync(session: UserSession): List<CharacterSyncConflict> {
            syncCount++
            return emptyList()
        }

        override suspend fun syncCharacter(session: UserSession, characterId: String): List<CharacterSyncConflict> {
            syncedCharacterIds += characterId
            onSyncCharacter?.invoke(characterId)
            return emptyList()
        }

        override suspend fun resolveConflict(
            session: UserSession,
            conflict: CharacterSyncConflict,
            remoteFieldIds: Set<String>,
        ) = Unit
    }

    private class FakeCampaignRepository : CampaignRepository {
        var createCount = 0
        override fun observe(session: UserSession): Flow<List<Campaign>> = flowOf(emptyList())
        override fun observeMembers(campaignId: String): Flow<List<CampaignMember>> = flowOf(emptyList())
        override fun observeMemberships(session: UserSession): Flow<List<CampaignMember>> = flowOf(emptyList())
        override fun observeInvites(campaignId: String): Flow<List<CampaignInvite>> = flowOf(emptyList())
        override fun observeAllInvites(): Flow<List<CampaignInvite>> = flowOf(emptyList())
        override suspend fun create(session: UserSession, name: String, description: String): Campaign {
            createCount++
            return Campaign(name = name, description = description, ownerId = session.uid)
        }

        override suspend fun update(session: UserSession, campaign: Campaign) = Unit
        override suspend fun archive(session: UserSession, campaign: Campaign, archived: Boolean) = Unit
        override suspend fun delete(session: UserSession, campaign: Campaign) = Unit
        override suspend fun leave(session: UserSession, campaign: Campaign) = Unit
        override suspend fun removeMember(session: UserSession, campaign: Campaign, userId: String) = Unit
        override suspend fun createInvite(session: UserSession, campaign: Campaign): CampaignInvite =
            CampaignInvite(code = "ABC123")

        override suspend fun previewInvite(session: UserSession, code: String): CampaignInvitePreview? = null
        override suspend fun joinByCode(session: UserSession, code: String): Campaign = Campaign()
        override suspend fun linkCharacter(session: UserSession, character: Character, campaign: Campaign): Character = character
        override suspend fun unlinkCharacter(session: UserSession, character: Character): Character = character
        override suspend fun sync(session: UserSession) = Unit
    }

    private class FakeOperationsRepository : OperationsRepository {
        var applyCount = 0
        override fun observeAudit(campaignIds: Set<String>): Flow<List<SessionOperation>> = flowOf(emptyList())
        override fun observeLibrary(campaignIds: Set<String>): Flow<List<CampaignLibraryEntry>> = flowOf(emptyList())
        override fun observeDeliveries(session: UserSession, campaignIds: Set<String>): Flow<List<CampaignDelivery>> = flowOf(emptyList())
        override fun observeAlertSettings(campaignIds: Set<String>): Flow<List<CampaignAlertSettings>> = flowOf(emptyList())
        override suspend fun apply(session: UserSession, character: Character, command: SessionCommand): Boolean {
            applyCount++
            return true
        }

        override suspend fun saveLibrary(session: UserSession, entry: CampaignLibraryEntry) = Unit
        override suspend fun duplicateLibrary(session: UserSession, entry: CampaignLibraryEntry) = Unit
        override suspend fun archiveLibrary(session: UserSession, entry: CampaignLibraryEntry, archived: Boolean) = Unit
        override suspend fun deliver(
            session: UserSession,
            entry: CampaignLibraryEntry,
            recipients: List<Character>,
            knowledgeMappings: Map<String, String>,
        ) = Unit

        override suspend fun respondToDelivery(session: UserSession, delivery: CampaignDelivery, accept: Boolean) = Unit
        override suspend fun saveAlertSettings(settings: CampaignAlertSettings) = Unit
        override suspend fun sync(
            session: UserSession,
            readableCampaignIds: Set<String>,
            writableCampaignIds: Set<String>,
        ) = Unit
    }

    private class FakeOwnerRepository : OwnerRepository {
        override fun observe(): Flow<List<UserProfile>> = flowOf(emptyList())
        override suspend fun sync(session: UserSession) = Unit
    }

    private class FakeCatalogRepository : CatalogRepository {
        override fun observe(): Flow<List<CatalogEntry>> = flowOf(emptyList())
        override suspend fun refreshBundledCatalog() = Unit
    }
}
